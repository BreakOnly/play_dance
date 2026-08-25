package com.dance.studio.service;

import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Course;
import com.dance.studio.entity.CourseLevel;
import com.dance.studio.entity.DanceType;
import com.dance.studio.entity.Teacher;
import com.dance.studio.mapper.CourseLevelMapper;
import com.dance.studio.mapper.CourseMapper;
import com.dance.studio.mapper.DanceTypeMapper;
import com.dance.studio.mapper.TeacherMapper;
import com.dance.studio.vo.CourseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基础数据：舞种 / 难度 / 课程 / 教师 的 CRUD。
 * 所有写操作带当前门店 store_id（多门店隔离）。
 */
@Service
@RequiredArgsConstructor
public class BasicDataService {

    private final DanceTypeMapper danceTypeMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;

    private Long store() { return ContextHolder.storeId(); }

    // ---------- 舞种 ----------
    public List<DanceType> listDanceTypes(boolean onlyEnabled) { return danceTypeMapper.listByStore(store(), onlyEnabled); }
    public DanceType saveDanceType(DanceType d) {
        d.setStoreId(store());
        if (d.getId() == null) danceTypeMapper.insert(d); else danceTypeMapper.update(d);
        return danceTypeMapper.findById(d.getId());
    }
    public void deleteDanceType(Long id) { danceTypeMapper.deleteById(id, store()); }

    // ---------- 难度 ----------
    public List<CourseLevel> listLevels(boolean onlyEnabled) { return courseLevelMapper.listByStore(store(), onlyEnabled); }
    public CourseLevel saveLevel(CourseLevel d) {
        d.setStoreId(store());
        if (d.getId() == null) courseLevelMapper.insert(d); else courseLevelMapper.update(d);
        return courseLevelMapper.findById(d.getId());
    }
    public void deleteLevel(Long id) { courseLevelMapper.deleteById(id, store()); }

    // ---------- 课程 ----------
    public List<CourseVo> listCourses(String type, Long danceTypeId, Integer status) {
        return courseMapper.list(store(), type, danceTypeId, status);
    }
    public CourseVo getCourse(Long id) { return courseMapper.findVoById(id); }
    public CourseVo saveCourse(Course c) {
        c.setStoreId(store());
        if (c.getId() == null) courseMapper.insert(c); else courseMapper.update(c);
        return courseMapper.findVoById(c.getId());
    }
    public void deleteCourse(Long id) { courseMapper.deleteById(id, store()); }

    // ---------- 教师 ----------
    public List<Teacher> listTeachers(boolean onlyEnabled) { return teacherMapper.listByStore(store(), onlyEnabled); }
    public Teacher saveTeacher(Teacher t) {
        t.setStoreId(store());
        if (t.getId() == null) teacherMapper.insert(t); else teacherMapper.update(t);
        return teacherMapper.findById(t.getId());
    }
    public void deleteTeacher(Long id) { teacherMapper.deleteById(id, store()); }
}
