package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Teacher;
import com.dance.studio.mapper.CourseLevelMapper;
import com.dance.studio.mapper.CourseMapper;
import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.mapper.DanceTypeMapper;
import com.dance.studio.mapper.TeacherMapper;
import com.dance.studio.service.ScheduleService;
import com.dance.studio.vo.CourseVo;
import com.dance.studio.vo.ScheduleVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "小程序-课表")
@RestController
@RequestMapping("/api/mp/schedules")
@RequiredArgsConstructor
public class MpScheduleController {

    private static final Map<String, String> COURSE_TYPE_LABELS =
            Map.of("group", "团课", "private", "私教");

    private final ScheduleService scheduleService;
    private final CourseScheduleMapper scheduleMapper;
    private final DanceTypeMapper danceTypeMapper;
    private final CourseLevelMapper levelMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;

    @Operation(summary = "课表(跨门店,支持关键字与舞种/难度/门店筛选)")
    @GetMapping
    public Result<List<ScheduleVo>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime end,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long danceTypeId,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) Long storeId) {
        return Result.success(
                scheduleMapper.listForMp(
                        start, end, keyword, danceTypeId, levelId, 1L, ContextHolder.userId()));
    }

    @Operation(summary = "课次详情")
    @GetMapping("/{id}")
    public Result<ScheduleVo> detail(@PathVariable Long id) {
        return Result.success(scheduleService.detailForUser(id, ContextHolder.userId()));
    }

    @Operation(summary = "约课筛选项(舞种/难易/类型/老师/课程,时段由前端固定)")
    @GetMapping("/filter-options")
    public Result<Map<String, Object>> filterOptions() {
        Long storeId = ContextHolder.storeId();
        List<CourseVo> courses = courseMapper.list(storeId, null, null, 1);

        // 课程类型取上架课程去重，映射中文标签
        List<Map<String, Object>> courseTypes = new ArrayList<>();
        courses.stream()
                .map(CourseVo::getType)
                .distinct()
                .forEach(
                        t -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("value", t);
                            m.put("label", COURSE_TYPE_LABELS.getOrDefault(t, t));
                            courseTypes.add(m);
                        });

        List<Map<String, Object>> teachers = new ArrayList<>();
        for (Teacher t : teacherMapper.listByStore(storeId, true)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("avatar", t.getAvatar());
            teachers.add(m);
        }

        List<Map<String, Object>> courseList = new ArrayList<>();
        for (CourseVo c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            courseList.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("danceTypes", danceTypeMapper.listByStore(storeId, true));
        data.put("levels", levelMapper.listByStore(storeId, true));
        data.put("courseTypes", courseTypes);
        data.put("teachers", teachers);
        data.put("courses", courseList);
        return Result.success(data);
    }
}
