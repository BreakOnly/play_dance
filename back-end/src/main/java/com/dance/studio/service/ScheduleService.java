package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.dto.ScheduleSaveReq;
import com.dance.studio.entity.Booking;
import com.dance.studio.entity.Course;
import com.dance.studio.entity.CourseSchedule;
import com.dance.studio.mapper.BookingMapper;
import com.dance.studio.mapper.CourseMapper;
import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.vo.ScheduleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CourseScheduleMapper scheduleMapper;
    private final CourseMapper courseMapper;
    private final BookingMapper bookingMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final NotificationService notificationService;

    private Long store() { return ContextHolder.storeId(); }

    public List<ScheduleVo> list(LocalDateTime start, LocalDateTime end, Long courseId, Long teacherId) {
        return scheduleMapper.list(store(), start, end, courseId, teacherId, null);
    }

    public ScheduleVo detail(Long id) {
        return scheduleMapper.findVoById(id, null);
    }

    /** 小程序：带当前学员约课状态 */
    public List<ScheduleVo> listForStudent(LocalDateTime start, LocalDateTime end, Long studentId) {
        return scheduleMapper.list(store(), start, end, null, null, studentId);
    }

    public ScheduleVo detailForStudent(Long id, Long studentId) {
        return scheduleMapper.findVoById(id, studentId);
    }

    public ScheduleVo save(Long id, ScheduleSaveReq req) {
        if (req.getEndTime().isBefore(req.getStartTime()) || req.getEndTime().isEqual(req.getStartTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        // 冲突检测（同教师/同教室时间重叠）
        int conflict = scheduleMapper.countConflict(store(), req.getTeacherId(), req.getRoom(),
                req.getStartTime(), req.getEndTime(), id);
        if (conflict > 0) {
            throw new BizException(ResultCode.SCHEDULE_CONFLICT);
        }
        // 容量默认12
        Integer capacity = req.getCapacity() == null ? 12 : req.getCapacity();
        CourseSchedule s = new CourseSchedule();
        s.setId(id);
        s.setStoreId(store());
        s.setCourseId(req.getCourseId());
        s.setTeacherId(req.getTeacherId());
        s.setRoom(req.getRoom());
        s.setSong(req.getSong());
        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setCapacity(capacity);
        if (id == null) {
            scheduleMapper.insert(s);
        } else {
            scheduleMapper.update(s);
        }
        return scheduleMapper.findVoById(s.getId(), null);
    }

    /**
     * 取消课次：流转状态，并批量取消所有"待上课/候补"预约、退还锁定课时、通知学员。
     * 课次取消属门店原因，一律免费退回（不受 6h 规则约束）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        CourseSchedule s = scheduleMapper.findById(id);
        if (s == null) throw new BizException(ResultCode.NOT_FOUND);
        if ("cancelled".equals(s.getStatus())) return;
        scheduleMapper.updateStatus(id, "cancelled");

        List<Booking> actives = bookingMapper.listActiveBySchedule(id);
        if (actives.isEmpty()) return;

        boolean lockOn = configService.getBool(s.getStoreId(), "lock_on_booking", true);
        Course c = courseMapper.findById(s.getCourseId());
        String courseName = c != null ? c.getName() : "";
        for (Booking b : actives) {
            // 条件取消：并发操作已改变状态则跳过
            if (bookingMapper.cancel(b.getId(), b.getStatus(), 0) == 0) continue;
            int cost = b.getLockedHours() == null ? 1 : b.getLockedHours();
            if (lockOn) {
                accountService.refundLock(b.getStudentId(), cost, b.getId());
            }
            notificationService.push(b.getStudentId(), "booking", "课程已取消",
                    "你预约的《" + courseName + "》课次已取消，课时已退回，抱歉带来不便。",
                    "booking", b.getId());
        }
    }
}
