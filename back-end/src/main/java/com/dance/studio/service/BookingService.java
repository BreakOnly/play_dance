package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Booking;
import com.dance.studio.entity.Course;
import com.dance.studio.entity.CourseSchedule;
import com.dance.studio.mapper.BookingMapper;
import com.dance.studio.mapper.CourseMapper;
import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.vo.BookingVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingMapper bookingMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final CourseMapper courseMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final NotificationService notificationService;

    /**
     * 约课。返回结果含状态（booked 成功 / waitlist 候补）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BookingVo book(Long studentId, Long scheduleId, boolean isAdmin) {
        CourseSchedule sch = scheduleMapper.findById(scheduleId);
        if (sch == null) throw new BizException(ResultCode.NOT_FOUND);
        if ("finished".equals(sch.getStatus()) || "cancelled".equals(sch.getStatus())) {
            throw new BizException(ResultCode.BOOK_WINDOW_INVALID);
        }
        Long storeId = sch.getStoreId();

        // 重复约课
        if (bookingMapper.findActiveByStudentSchedule(studentId, scheduleId) != null) {
            throw new BizException(ResultCode.BOOK_DUPLICATE);
        }

        // 约课时间窗（管理员代约可放宽）
        if (!isAdmin) {
            LocalDateTime now = LocalDateTime.now();
            int aheadDays = configService.getInt(storeId, "book_ahead_days", 7);
            int cutoffMin = configService.getInt(storeId, "book_cutoff_min", 30);
            if (sch.getStartTime().isBefore(now.plusMinutes(cutoffMin))) {
                throw new BizException(ResultCode.BOOK_WINDOW_INVALID);
            }
            if (sch.getStartTime().isAfter(now.plusDays(aheadDays))) {
                throw new BizException(ResultCode.BOOK_WINDOW_INVALID);
            }
        }

        Course course = courseMapper.findById(sch.getCourseId());
        int cost = course != null && course.getCostHours() != null ? course.getCostHours() : 1;

        // 抢占名额
        boolean gotSlot = scheduleMapper.incrBooked(scheduleId) > 0;
        String status;
        Integer waitlistNo = null;
        if (gotSlot) {
            status = "booked";
        } else {
            int limit = configService.getInt(storeId, "waitlist_limit", 5);
            int wl = bookingMapper.countWaitlist(scheduleId);
            if (wl >= limit) throw new BizException(ResultCode.BOOK_WAITLIST_FULL);
            status = "waitlist";
            waitlistNo = wl + 1;
        }

        Booking b = new Booking();
        b.setStoreId(storeId);
        b.setStudentId(studentId);
        b.setScheduleId(scheduleId);
        b.setStatus(status);
        b.setLockedHours(cost);
        b.setWaitlistNo(waitlistNo);
        b.setIsAdmin(isAdmin ? 1 : 0);
        bookingMapper.insert(b);

        // 锁定课时（待上课与候补都锁定，保证候补转正时课时充足）
        // lock_on_booking=0 时约课不锁课时，改在签到时从可用余额扣减
        if (configService.getBool(storeId, "lock_on_booking", true)) {
            accountService.lockHours(studentId, cost, b.getId());
        }

        return bookingMapper.listByStudent(studentId, null).stream()
                .filter(v -> v.getId().equals(b.getId())).findFirst().orElse(null);
    }

    /**
     * 取消/请假。6h 规则：≥阈值免费退回；否则扣课时。候补取消则直接退回。
     * 并发安全：状态流转用条件 UPDATE，仅当更新影响行数>0 才执行课时退/扣，避免重复退款。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long bookingId, Long studentId, boolean isAdmin) {
        Booking b = bookingMapper.findById(bookingId);
        if (b == null) throw new BizException(ResultCode.BOOK_NOT_FOUND);
        if (!isAdmin && !b.getStudentId().equals(studentId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        int cost = b.getLockedHours() == null ? 1 : b.getLockedHours();
        // lock_on_booking=0 时约课未锁课时，取消无课时可退/可扣
        boolean lockOn = configService.getBool(b.getStoreId(), "lock_on_booking", true);

        if ("waitlist".equals(b.getStatus())) {
            int rows = bookingMapper.cancel(bookingId, "waitlist", 0);
            if (rows == 0) throw new BizException(ResultCode.CANCEL_NOT_ALLOWED);
            if (lockOn) accountService.refundLock(b.getStudentId(), cost, bookingId);
            return;
        }
        if (!"booked".equals(b.getStatus())) {
            throw new BizException(ResultCode.CANCEL_NOT_ALLOWED);
        }

        CourseSchedule sch = scheduleMapper.findById(b.getScheduleId());
        int freeHours = configService.getInt(b.getStoreId(), "cancel_free_hours", 6);
        boolean free = isAdmin || Duration.between(LocalDateTime.now(), sch.getStartTime()).toMinutes() >= freeHours * 60L;

        int rows = bookingMapper.cancel(bookingId, "booked", free ? 0 : 1);
        if (rows == 0) throw new BizException(ResultCode.CANCEL_NOT_ALLOWED);
        if (lockOn) {
            if (free) {
                accountService.refundLock(b.getStudentId(), cost, bookingId);
            } else {
                accountService.forfeitOnNoShow(b.getStudentId(), cost, bookingId, false);
            }
        }
        // 释放名额并尝试候补递补
        scheduleMapper.decrBooked(b.getScheduleId());
        promoteWaitlist(b.getScheduleId());
    }

    /** 候补自动递补：取下一个候补转正并占位 */
    private void promoteWaitlist(Long scheduleId) {
        Booking next = bookingMapper.nextWaitlist(scheduleId);
        if (next == null) return;
        if (scheduleMapper.incrBooked(scheduleId) > 0) {
            if (bookingMapper.promote(next.getId()) > 0) {
                notificationService.push(next.getStudentId(), "booking", "候补转正成功",
                        "你候补的课程已为你预留名额，记得按时上课～", "booking", next.getId());
            } else {
                // 候补在查询与转正之间被并发取消，归还已占名额
                scheduleMapper.decrBooked(scheduleId);
            }
        }
    }

    /** 爽约处理（定时任务调用）：课次已结束仍"待上课" → 爽约扣课时 */
    @Transactional(rollbackFor = Exception.class)
    public void handleNoShow(Long bookingId) {
        Booking b = bookingMapper.findById(bookingId);
        if (b == null || !"booked".equals(b.getStatus())) return;
        // 条件流转：并发取消/签到已改变状态时不重复处理
        if (bookingMapper.updateStatusIf(b.getId(), "noshow", "booked") == 0) return;
        int cost = b.getLockedHours() == null ? 1 : b.getLockedHours();
        // lock_on_booking=0 时未锁课时，爽约无课时可扣
        if (configService.getBool(b.getStoreId(), "lock_on_booking", true)) {
            accountService.forfeitOnNoShow(b.getStudentId(), cost, b.getId(), true);
        }
        scheduleMapper.decrBooked(b.getScheduleId());
    }

    public List<Long> noShowCandidateIds() {
        return bookingMapper.listNoShowCandidates(LocalDateTime.now()).stream().map(Booking::getId).toList();
    }

    public List<BookingVo> myBookings(Long studentId, String status) {
        return bookingMapper.listByStudent(studentId, status);
    }

    public List<BookingVo> bySchedule(Long scheduleId) {
        return bookingMapper.listBySchedule(scheduleId);
    }
}
