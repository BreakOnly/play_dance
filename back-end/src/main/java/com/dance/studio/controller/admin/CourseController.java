package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.entity.Course;
import com.dance.studio.service.BasicDataService;
import com.dance.studio.vo.CourseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-课程")
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseController {

    private final BasicDataService service;

    @Operation(summary = "列表(可按 type/danceTypeId/status 过滤)")
    @GetMapping
    public Result<List<CourseVo>> list(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) Long danceTypeId,
                                       @RequestParam(required = false) Integer status) {
        return Result.success(service.listCourses(type, danceTypeId, status));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<CourseVo> detail(@PathVariable Long id) {
        return Result.success(service.getCourse(id));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<CourseVo> create(@RequestBody Course c) {
        c.setId(null);
        return Result.success(service.saveCourse(c));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<CourseVo> update(@PathVariable Long id, @RequestBody Course c) {
        c.setId(id);
        return Result.success(service.saveCourse(c));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteCourse(id);
        return Result.success();
    }
}
