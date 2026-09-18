package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.dto.ScheduleSaveReq;
import com.dance.studio.entity.Booking;
import com.dance.studio.entity.Course;
import com.dance.studio.entity.CourseSchedule;
import com.dance.studio.entity.Room;
import com.dance.studio.mapper.BookingMapper;
import com.dance.studio.mapper.CourseMapper;
import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.mapper.RoomMapper;
import com.dance.studio.vo.ScheduleVo;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CourseScheduleMapper scheduleMapper;
    private final CourseMapper courseMapper;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final NotificationService notificationService;
    private final StudioLock lock;
    private final com.dance.studio.mapper.TeacherMapper teachers;
    private final com.dance.studio.mapper.NoticeMapper notices;

    private Long store() {
        return ContextHolder.storeId();
    }

    public List<ScheduleVo> list(
            LocalDateTime start, LocalDateTime end, Long courseId, Long teacherId) {
        return scheduleMapper.list(store(), start, end, courseId, teacherId, null);
    }

    public ScheduleVo detail(Long id) {
        return scheduleMapper.findVoById(id, null);
    }

    /** 小程序：带当前学员约课状态 */
    public List<ScheduleVo> listForUser(LocalDateTime start, LocalDateTime end, Long userId) {
        return scheduleMapper.list(store(), start, end, null, null, userId);
    }

    public ScheduleVo detailForUser(Long id, Long userId) {
        return scheduleMapper.findVoById(id, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScheduleVo save(Long id, ScheduleSaveReq req) {
        lock.acquire();
        if (req.getStartTime() == null || req.getEndTime() == null || req.getCourseId() == null)
            throw new BizException("请完整填写课次时间与课程");
        CourseSchedule old = id == null ? null : scheduleMapper.findById(id);
        if (id != null && (old == null || !store().equals(old.getStoreId())))
            throw new BizException(ResultCode.NOT_FOUND);
        if (old != null
                && (!old.getStartTime().isAfter(LocalDateTime.now())
                        || "cancelled".equals(old.getStatus())))
            throw new BizException("已开始或已取消课次不能编辑");
        if (old != null
                && old.getBookedCount() > 0
                && (!old.getCourseId().equals(req.getCourseId())
                        || !java.util.Objects.equals(old.getTeacherId(), req.getTeacherId())
                        || !old.getStartTime().equals(req.getStartTime())
                        || !old.getEndTime().equals(req.getEndTime())
                        || !java.util.Objects.equals(old.getRoom(), req.getRoom())))
            throw new BizException("已有预约，请先取消原课次并通知学员，再重新排课");
        if (!req.getStartTime().isAfter(LocalDateTime.now())) throw new BizException("不能排过去的课程");
        var teacher = req.getTeacherId() == null ? null : teachers.findById(req.getTeacherId());
        if (teacher == null || teacher.getStatus() != 1 || !store().equals(teacher.getStoreId()))
            throw new BizException("请选择启用的教师");
        if (req.getNoticeId() != null) {
            var n = notices.findById(req.getNoticeId());
            if (n == null || n.getStatus() != 1 || !store().equals(n.getStoreId()))
                throw new BizException("注意事项不可用");
        }
        if (req.getEndTime().isBefore(req.getStartTime())
                || req.getEndTime().isEqual(req.getStartTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        // 课程必须存在，课次时长必须与课程时长一致（时长属于课程定义，排课不可改）
        Course course = courseMapper.findById(req.getCourseId());
        if (course == null || course.getStatus() != 1 || !store().equals(course.getStoreId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "课程不存在");
        }
        long plannedMin = Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
        if (plannedMin != course.getDurationMin()) {
            throw new BizException(
                    ResultCode.PARAM_ERROR.getCode(),
                    "课次时长必须与课程时长一致(" + course.getDurationMin() + "分钟)，如需不同时长请新建课程");
        }
        // 教室必须来自字典（启用状态），避免手填写法不一致绕过冲突校验
        Room room = null;
        if (req.getRoom() != null && !req.getRoom().isBlank()) {
            room =
                    roomMapper.listByStore(store(), true).stream()
                            .filter(r -> r.getName().equals(req.getRoom()))
                            .findFirst()
                            .orElse(null);
            if (room == null) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "教室不存在，请先在课程管理的教室字典中维护");
            }
        }
        // 冲突检测（同教师/同教室时间重叠）
        int conflict =
                scheduleMapper.countConflict(
                        store(),
                        req.getTeacherId(),
                        req.getRoom(),
                        req.getStartTime(),
                        req.getEndTime(),
                        id);
        if (conflict > 0) {
            throw new BizException(ResultCode.SCHEDULE_CONFLICT);
        }
        // 容量默认取系统设置中该课程类型的默认容量
        Integer capacity = req.getCapacity();
        if (capacity == null) {
            String key =
                    "private".equals(course.getType())
                            ? "default_capacity_private"
                            : "default_capacity_group";
            capacity =
                    "private".equals(course.getType()) ? 1 : configService.getInt(store(), key, 12);
        }
        if (capacity < 1 || capacity > 200 || ("private".equals(course.getType()) && capacity != 1))
            throw new BizException("容量不正确，私教固定为 1 人");
        if (old != null && capacity < old.getBookedCount()) throw new BizException("容量不能低于已预约人数");
        // 容量不得超过教室最大容纳人数(0=不限制)
        if (room != null
                && room.getMaxCapacity() != null
                && room.getMaxCapacity() > 0
                && capacity > room.getMaxCapacity()) {
            throw new BizException(
                    ResultCode.PARAM_ERROR.getCode(),
                    "容量超过" + room.getName() + "最大容纳人数(" + room.getMaxCapacity() + ")");
        }
        CourseSchedule s = new CourseSchedule();
        s.setId(id);
        s.setStoreId(store());
        s.setCourseId(req.getCourseId());
        s.setTeacherId(req.getTeacherId());
        s.setRoom(req.getRoom());
        s.setSong(req.getSong());
        s.setNoticeId(req.getNoticeId());
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

    /** 取消课次：流转状态，并批量取消所有"待上课/候补"预约、退还仍有效的已扣课时、通知学员。 课次取消属门店原因，一律免费退回（不受 6h 规则约束）。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        lock.acquire();
        CourseSchedule s = scheduleMapper.findById(id);
        if (s == null) throw new BizException(ResultCode.NOT_FOUND);
        if ("cancelled".equals(s.getStatus())) return;
        scheduleMapper.updateStatus(id, "cancelled");

        List<Booking> actives = bookingMapper.listActiveBySchedule(id);
        if (actives.isEmpty()) return;

        Course c = courseMapper.findById(s.getCourseId());
        String courseName = c != null ? c.getName() : "";
        for (Booking b : actives) {
            // 条件取消：并发操作已改变状态则跳过
            if (bookingMapper.cancel(b.getId(), b.getStatus(), 0) == 0) continue;
            int cost = b.getCostHours() == null ? 1 : b.getCostHours();
            if ("booked".equals(b.getStatus())) {
                accountService.refundBooking(b.getUserId(), cost, b.getId());
            }
            notificationService.push(
                    b.getUserId(),
                    "booking",
                    "课程已取消",
                    "你预约的《" + courseName + "》课次已取消，仍在有效期内的课时已退回，抱歉带来不便。",
                    "booking",
                    b.getId());
        }
    }
}
