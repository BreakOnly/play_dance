package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Booking;
import com.dance.studio.entity.Checkin;
import com.dance.studio.entity.User;
import com.dance.studio.mapper.BookingMapper;
import com.dance.studio.mapper.CheckinMapper;
import com.dance.studio.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final BookingMapper bookingMapper;
    private final CheckinMapper checkinMapper;
    private final UserMapper userMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final StudioLock lock;
    private final com.dance.studio.mapper.CourseScheduleMapper scheduleMapper;

    /** 管理员代签（到店免扫码，工作人员后台确认签到） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> manualCheckin(Long bookingId, Long operatorId) {
        lock.acquire();
        Booking b = bookingMapper.findById(bookingId);
        if (b == null) throw new BizException(ResultCode.BOOK_NOT_FOUND);
        if (!Long.valueOf(1).equals(b.getStoreId())) throw new BizException(ResultCode.FORBIDDEN);
        if (scheduleMapper
                .findById(b.getScheduleId())
                .getStartTime()
                .isAfter(
                        LocalDateTime.now()
                                .plusMinutes(configService.getInt(1L, "checkin_before_min", 30))))
            throw new BizException("尚未到签到时间");
        return doCheckin(b, "manual", operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanCheckin(Long scheduleId, Long userId) {
        lock.acquire();
        var schedule = scheduleMapper.findById(scheduleId);
        if (schedule == null
                || !Long.valueOf(1).equals(schedule.getStoreId())
                || "cancelled".equals(schedule.getStatus())) throw new BizException("课程不存在或已取消");
        Booking b = bookingMapper.findActiveByUserSchedule(userId, scheduleId);
        if (b == null) throw new BizException(ResultCode.CHECKIN_NO_BOOKING);
        if ("checked".equals(b.getStatus())) return receipt(b);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(
                        schedule.getStartTime()
                                .minusMinutes(configService.getInt(1L, "checkin_before_min", 30)))
                || !now.isBefore(schedule.getEndTime())) throw new BizException("不在签到时段，请联系工作人员");
        return doCheckin(b, "scan", userId);
    }

    private Map<String, Object> receipt(Booking b) {
        User user = userMapper.findById(b.getUserId());
        return Map.of(
                "bookingId",
                b.getId(),
                "costHours",
                b.getLockedHours(),
                "pointsGain",
                b.getCheckinPoints(),
                "balanceAfter",
                user.getCourseBalance(),
                "alreadyChecked",
                true);
    }

    private Map<String, Object> doCheckin(Booking b, String method, Long operatorId) {
        if ("checked".equals(b.getStatus()) || checkinMapper.findByBooking(b.getId()) != null) {
            return receipt(b);
        }
        if (!"booked".equals(b.getStatus())) {
            throw new BizException(ResultCode.CHECKIN_NO_BOOKING);
        }
        // 条件流转：并发取消/签到已改变状态时不重复核销
        if (bookingMapper.updateStatusIf(b.getId(), "checked", "booked") == 0) {
            Booking cur = bookingMapper.findById(b.getId());
            throw new BizException(
                    cur != null && "checked".equals(cur.getStatus())
                            ? ResultCode.CHECKIN_DUPLICATE
                            : ResultCode.CHECKIN_NO_BOOKING);
        }
        int cost = b.getLockedHours() == null ? 1 : b.getLockedHours();
        int points = b.getCheckinPoints();

        // 1. 课时核销：lock_on_booking=1 消耗冻结；=0 时约课未锁定，从可用余额直接扣
        accountService.consumeOnCheckin(b.getUserId(), cost, b.getId());
        // 2. 加积分
        accountService.earnPoints(b.getUserId(), points, "checkin", "checkin", b.getId(), "签到积分");
        // 4. 写签到记录
        Checkin c = new Checkin();
        c.setStoreId(b.getStoreId());
        c.setBookingId(b.getId());
        c.setUserId(b.getUserId());
        c.setScheduleId(b.getScheduleId());
        c.setMethod(method);
        c.setOperatorId(operatorId);
        checkinMapper.insert(c);

        // 5. 查签到后剩余可用课时（供前端展示）
        User stu = userMapper.findById(b.getUserId());
        int balanceAfter =
                (stu != null && stu.getCourseBalance() != null) ? stu.getCourseBalance() : 0;

        return Map.of(
                "bookingId", b.getId(),
                "costHours", cost,
                "pointsGain", points,
                "balanceAfter", balanceAfter,
                "checkinTime", LocalDateTime.now().toString());
    }
}
