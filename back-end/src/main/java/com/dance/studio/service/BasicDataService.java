package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Course;
import com.dance.studio.entity.CourseLevel;
import com.dance.studio.entity.DanceType;
import com.dance.studio.entity.Notice;
import com.dance.studio.entity.Room;
import com.dance.studio.entity.Teacher;
import com.dance.studio.mapper.CourseLevelMapper;
import com.dance.studio.mapper.CourseMapper;
import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.mapper.DanceTypeMapper;
import com.dance.studio.mapper.NoticeMapper;
import com.dance.studio.mapper.RoomMapper;
import com.dance.studio.mapper.TeacherMapper;
import com.dance.studio.vo.CourseVo;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/** 基础数据：舞种 / 难度 / 课程 / 教师 的 CRUD。 所有写操作带当前门店 store_id（多门店隔离）。 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
public class BasicDataService {
    private final StudioLock lock;

    private final DanceTypeMapper danceTypeMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final RoomMapper roomMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final NoticeMapper noticeMapper;
    private final CourseScheduleMapper courseScheduleMapper;

    private Long store() {
        return ContextHolder.storeId();
    }

    // ---------- 舞种 ----------
    public List<DanceType> listDanceTypes(boolean onlyEnabled) {
        return danceTypeMapper.listByStore(store(), onlyEnabled);
    }

    public DanceType saveDanceType(DanceType d) {
        d.setStoreId(store());
        if (d.getId() == null) danceTypeMapper.insert(d);
        else danceTypeMapper.update(d);
        return danceTypeMapper.findById(d.getId());
    }

    public void deleteDanceType(Long id) {
        danceTypeMapper.deleteById(id, store());
    }

    // ---------- 难度 ----------
    public List<CourseLevel> listLevels(boolean onlyEnabled) {
        return courseLevelMapper.listByStore(store(), onlyEnabled);
    }

    /** 保存星级。校验：星级必须在 1~5 之间、全店不重复，且最多 5 条（1~5 星各一条）。 */
    public CourseLevel saveLevel(CourseLevel d) {
        if (d.getSort() == null || d.getSort() < 1 || d.getSort() > 5) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "星级必须在 1~5 之间");
        }
        // 同一星级只允许一条（编辑时排除自身）
        if (courseLevelMapper.countBySort(store(), d.getSort(), d.getId()) > 0) {
            throw new BizException(
                    ResultCode.PARAM_ERROR.getCode(), "星级 " + d.getSort() + " 已存在，每个星级只能有一条数据");
        }
        d.setStoreId(store());
        if (d.getId() == null) {
            // 最多 5 条：1~5 星各一条
            if (courseLevelMapper.listByStore(store(), false).size() >= 5) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "最多只能有 5 个星级（1~5 星各一条）");
            }
            courseLevelMapper.insert(d);
        } else {
            courseLevelMapper.update(d);
        }
        return courseLevelMapper.findById(d.getId());
    }

    public void deleteLevel(Long id) {
        courseLevelMapper.deleteById(id, store());
    }

    // ---------- 教室 ----------
    public List<Room> listRooms(boolean onlyEnabled) {
        return roomMapper.listByStore(store(), onlyEnabled);
    }

    public Room saveRoom(Room d) {
        d.setStoreId(store());
        if (d.getId() == null) roomMapper.insert(d);
        else roomMapper.update(d);
        return roomMapper.findById(d.getId());
    }

    public void deleteRoom(Long id) {
        roomMapper.deleteById(id, store());
    }

    // ---------- 课程 ----------
    public List<CourseVo> listCourses(String type, Long danceTypeId, Integer status) {
        return courseMapper.list(store(), type, danceTypeId, status);
    }

    public CourseVo getCourse(Long id) {
        return courseMapper.findVoById(id);
    }

    public CourseVo saveCourse(Course c) {
        lock.acquire();
        if (c.getName() == null
                || c.getName().isBlank()
                || c.getName().length() > 100
                || c.getCostHours() == null
                || c.getCostHours() < 1
                || c.getCostHours() > 100
                || c.getDurationMin() == null
                || c.getDurationMin() < 1
                || c.getDurationMin() > 480
                || !java.util.List.of("group", "private").contains(c.getType()))
            throw new BizException("请填写有效课程名称、类型、时长和课时数");
        if (c.getPrice() != null && (c.getPrice().signum() < 0 || c.getPrice().scale() > 2))
            throw new BizException("价格必须非负且最多两位小数");
        if (c.getDanceTypeId() == null
                || danceTypeMapper.findById(c.getDanceTypeId()) == null
                || c.getLevelId() == null
                || courseLevelMapper.findById(c.getLevelId()) == null)
            throw new BizException("舞种或难度不存在");
        c.setStoreId(store());
        if (c.getId() == null) courseMapper.insert(c);
        else courseMapper.update(c);
        return courseMapper.findVoById(c.getId());
    }

    public void deleteCourse(Long id) {
        lock.acquire();
        courseMapper.deleteById(id, store());
    }

    // ---------- 教师 ----------
    public List<Teacher> listTeachers(boolean onlyEnabled) {
        return teacherMapper.listByStore(store(), onlyEnabled);
    }

    public Teacher saveTeacher(Teacher t) {
        t.setStoreId(store());
        if (t.getId() == null) teacherMapper.insert(t);
        else teacherMapper.update(t);
        return teacherMapper.findById(t.getId());
    }

    public void deleteTeacher(Long id) {
        teacherMapper.deleteById(id, store());
    }

    // ---------- 注意事项 ----------
    public List<Notice> listNotices(boolean onlyEnabled) {
        return noticeMapper.listByStore(store(), onlyEnabled);
    }

    public Notice saveNotice(Notice n) {
        n.setStoreId(store());
        if (n.getName() == null || n.getName().isBlank())
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "注意事项名称不能为空");
        if (n.getContent() == null || n.getContent().isBlank())
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "注意事项内容不能为空");
        if (n.getId() == null) noticeMapper.insert(n);
        else noticeMapper.update(n);
        return noticeMapper.findById(n.getId());
    }

    public void deleteNotice(Long id) {
        // 被排课引用的注意事项不允许删除，先在排课里改选其他/清空
        int used = courseScheduleMapper.countByNotice(id, store());
        if (used > 0)
            throw new BizException(
                    ResultCode.PARAM_ERROR.getCode(), "该注意事项已被 " + used + " 节排课引用，请先调整排课");
        noticeMapper.deleteById(id, store());
    }
}
