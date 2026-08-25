package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Booking;
import com.dance.studio.entity.Checkin;
import com.dance.studio.entity.Student;
import com.dance.studio.mapper.BookingMapper;
import com.dance.studio.mapper.CheckinMapper;
import com.dance.studio.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final BookingMapper bookingMapper;
    private final CheckinMapper checkinMapper;
    private final StudentMapper studentMapper;
    private final AccountService accountService;
    private final ConfigService configService;

    /** 管理员代签（到店免扫码，工作人员后台确认签到） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> manualCheckin(Long bookingId, Long operatorId) {
        Booking b = bookingMapper.findById(bookingId);
        if (b == null) throw new BizException(ResultCode.BOOK_NOT_FOUND);
        return doCheckin(b, "manual", operatorId);
    }

    private Map<String, Object> doCheckin(Booking b, String method, Long operatorId) {
        if ("checked".equals(b.getStatus()) || checkinMapper.findByBooking(b.getId()) != null) {
            throw new BizException(ResultCode.CHECKIN_DUPLICATE);
        }
        if (!"booked".equals(b.getStatus())) {
            throw new BizException(ResultCode.CHECKIN_NO_BOOKING);
        }
        // 条件流转：并发取消/签到已改变状态时不重复核销
        if (bookingMapper.updateStatusIf(b.getId(), "checked", "booked") == 0) {
            Booking cur = bookingMapper.findById(b.getId());
            throw new BizException(cur != null && "checked".equals(cur.getStatus())
                    ? ResultCode.CHECKIN_DUPLICATE : ResultCode.CHECKIN_NO_BOOKING);
        }
        int cost = b.getLockedHours() == null ? 1 : b.getLockedHours();
        int points = configService.getInt(b.getStoreId(), "points_per_checkin", 10);

        // 1. 课时核销：lock_on_booking=1 消耗冻结；=0 时约课未锁定，从可用余额直接扣
        if (configService.getBool(b.getStoreId(), "lock_on_booking", true)) {
            accountService.consumeOnCheckin(b.getStudentId(), cost, b.getId());
        } else {
            accountService.consumeDirect(b.getStudentId(), cost, b.getId());
        }
        // 2. 加积分
        accountService.earnPoints(b.getStudentId(), points, "checkin", "checkin", b.getId(), "签到积分");
        // 4. 写签到记录
        Checkin c = new Checkin();
        c.setStoreId(b.getStoreId());
        c.setBookingId(b.getId());
        c.setStudentId(b.getStudentId());
        c.setScheduleId(b.getScheduleId());
        c.setMethod(method);
        c.setOperatorId(operatorId);
        checkinMapper.insert(c);

        // 5. 查签到后剩余可用课时（供前端展示）
        Student stu = studentMapper.findById(b.getStudentId());
        int balanceAfter = (stu != null && stu.getCourseBalance() != null) ? stu.getCourseBalance() : 0;

        return Map.of(
                "bookingId", b.getId(),
                "costHours", cost,
                "pointsGain", points,
                "balanceAfter", balanceAfter,
                "checkinTime", LocalDateTime.now().toString()
        );
    }
}
