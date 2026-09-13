package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.*;
import com.dance.studio.mapper.*;
import com.dance.studio.vo.BookingVo;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingMapper bookingMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final CourseMapper courseMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final NotificationService notificationService;
    private final StudioLock lock;

    @Transactional(rollbackFor = Exception.class)
    public BookingVo book(Long userId, Long scheduleId, boolean isAdmin) {
        lock.acquire();
        CourseSchedule s = scheduleMapper.findById(scheduleId);
        if (s == null || !Objects.equals(s.getStoreId(), 1L))
            throw new BizException(ResultCode.NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        if (!List.of("open", "full").contains(s.getStatus()) || !s.getStartTime().isAfter(now))
            throw new BizException(ResultCode.BOOK_WINDOW_INVALID);
        Booking existing = bookingMapper.findActiveByUserSchedule(userId, scheduleId);
        if (existing != null)
            return myBookings(userId, null).stream()
                    .filter(b -> b.getId().equals(existing.getId()))
                    .findFirst()
                    .orElseThrow();
        if (!isAdmin
                && (s.getStartTime()
                                .isBefore(
                                        now.plusMinutes(
                                                configService.getInt(1L, "book_cutoff_min", 30)))
                        || s.getStartTime()
                                .isAfter(
                                        now.plusDays(
                                                configService.getInt(1L, "book_ahead_days", 7)))))
            throw new BizException(ResultCode.BOOK_WINDOW_INVALID);
        Course c = courseMapper.findById(s.getCourseId());
        if (c == null || c.getStatus() != 1 || c.getCostHours() == null || c.getCostHours() < 1)
            throw new BizException("课程不可预约");
        accountService.balances(userId);
        boolean booked = scheduleMapper.incrBooked(scheduleId) == 1;
        if (!booked
                && bookingMapper.countWaitlist(scheduleId)
                        >= configService.getInt(1L, "waitlist_limit", 5))
            throw new BizException(ResultCode.BOOK_WAITLIST_FULL);
        Booking b = new Booking();
        b.setStoreId(1L);
        b.setUserId(userId);
        b.setScheduleId(scheduleId);
        b.setStatus(booked ? "booked" : "waitlist");
        b.setLockedHours(booked ? c.getCostHours() : 0);
        b.setCostHours(c.getCostHours());
        b.setIsAdmin(isAdmin ? 1 : 0);
        b.setCheckinPoints(configService.getInt(1L, "points_per_checkin", 100));
        b.setWaitlistNo(booked ? null : bookingMapper.nextWaitlistNumber(scheduleId));
        bookingMapper.insert(b);
        if (booked) accountService.lockHours(userId, b.getLockedHours(), b.getId());
        return myBookings(userId, null).stream()
                .filter(v -> v.getId().equals(b.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long bookingId, Long userId, boolean isAdmin) {
        lock.acquire();
        Booking b = bookingMapper.findById(bookingId);
        if (b == null || !Objects.equals(b.getStoreId(), 1L))
            throw new BizException(ResultCode.BOOK_NOT_FOUND);
        if (!isAdmin && !Objects.equals(userId, b.getUserId()))
            throw new BizException(ResultCode.FORBIDDEN);
        if ("cancelled".equals(b.getStatus())) return;
        if (!List.of("booked", "waitlist").contains(b.getStatus()))
            throw new BizException(ResultCode.CANCEL_NOT_ALLOWED);
        CourseSchedule s = scheduleMapper.findById(b.getScheduleId());
        if (!isAdmin
                && "booked".equals(b.getStatus())
                && s.getStartTime()
                        .isBefore(
                                LocalDateTime.now()
                                        .plusHours(
                                                configService.getInt(1L, "cancel_free_hours", 6))))
            throw new BizException("临近开课不可在线取消，请联系管理员处理");
        if (bookingMapper.cancel(b.getId(), b.getStatus(), 0) != 1)
            throw new BizException(ResultCode.CANCEL_NOT_ALLOWED);
        if (b.getLockedHours() > 0)
            accountService.refundLock(b.getUserId(), b.getLockedHours(), b.getId());
        if ("booked".equals(b.getStatus())) {
            scheduleMapper.decrBooked(s.getId());
            promoteWaitlist(s.getId());
        }
    }

    private void promoteWaitlist(Long scheduleId) {
        CourseSchedule s = scheduleMapper.findById(scheduleId);
        if (!List.of("open", "full").contains(s.getStatus())
                || !s.getStartTime().isAfter(LocalDateTime.now())) return;
        Booking next;
        while ((next = bookingMapper.nextWaitlist(scheduleId)) != null) {
            int cost = next.getCostHours();
            if (next.getLockedHours() > 0) {
                accountService.refundLock(next.getUserId(), next.getLockedHours(), next.getId());
                bookingMapper.clearWaitlistLock(next.getId());
            }
            if (accountService.balances(next.getUserId()).get("available") < cost) {
                bookingMapper.cancel(next.getId(), "waitlist", 0);
                notificationService.push(
                        next.getUserId(),
                        "booking",
                        "候补未转正",
                        "有效课时不足，本次候补已结束。",
                        "booking",
                        next.getId());
                continue;
            }
            if (scheduleMapper.incrBooked(scheduleId) != 1) return;
            if (bookingMapper.promoteWithHours(next.getId(), cost) != 1)
                throw new BizException("候补状态已变化，请重试");
            accountService.lockHours(next.getUserId(), cost, next.getId());
            notificationService.push(
                    next.getUserId(),
                    "booking",
                    "候补转正成功",
                    "已为你预留名额并冻结课时，请按时到店。",
                    "booking",
                    next.getId());
            return;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleNoShow(Long bookingId) {
        lock.acquire();
        Booking b = bookingMapper.findById(bookingId);
        if (b == null || !"booked".equals(b.getStatus())) return;
        CourseSchedule s = scheduleMapper.findById(b.getScheduleId());
        if (s.getEndTime().isAfter(LocalDateTime.now())) return;
        if (bookingMapper.updateStatusIf(b.getId(), "noshow", "booked") != 1) return;
        accountService.forfeitOnNoShow(b.getUserId(), b.getLockedHours(), b.getId(), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void finishExpired() {
        lock.acquire();
        for (Booking b : bookingMapper.expiredWaitlists()) {
            if (bookingMapper.cancel(b.getId(), "waitlist", 0) != 1) continue;
            if (b.getLockedHours() > 0)
                accountService.refundLock(b.getUserId(), b.getLockedHours(), b.getId());
            notificationService.push(
                    b.getUserId(),
                    "booking",
                    "候补已结束",
                    "课程已开始，本次候补未转正，未扣除课时。",
                    "booking",
                    b.getId());
        }
        scheduleMapper.finishExpired(LocalDateTime.now());
    }

    public List<Long> noShowCandidateIds() {
        return bookingMapper.listNoShowCandidates(LocalDateTime.now()).stream()
                .map(Booking::getId)
                .toList();
    }

    public List<BookingVo> myBookings(Long userId, String status) {
        return bookingMapper.listByUser(userId, status);
    }

    public List<BookingVo> bySchedule(Long id) {
        return bookingMapper.listBySchedule(id);
    }
}
