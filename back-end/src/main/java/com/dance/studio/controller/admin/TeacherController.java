package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.entity.Teacher;
import com.dance.studio.service.BasicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-教师")
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final BasicDataService service;

    @Operation(summary = "列表")
    @GetMapping
    public Result<List<Teacher>> list() {
        return Result.success(service.listTeachers(false));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<Teacher> create(@RequestBody Teacher t) {
        t.setId(null);
        return Result.success(service.saveTeacher(t));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<Teacher> update(@PathVariable Long id, @RequestBody Teacher t) {
        t.setId(id);
        return Result.success(service.saveTeacher(t));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteTeacher(id);
        return Result.success();
    }
}
