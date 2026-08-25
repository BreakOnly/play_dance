package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.ResultCode;
import com.dance.studio.entity.CourseLevel;
import com.dance.studio.service.BasicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-星级")
@RestController
@RequestMapping("/api/admin/levels")
@RequiredArgsConstructor
public class LevelController {

    private final BasicDataService service;

    @Operation(summary = "列表")
    @GetMapping
    public Result<List<CourseLevel>> list() {
        return Result.success(service.listLevels(false));
    }

    @Operation(summary = "新增(最多5个)")
    @PostMapping
    public Result<CourseLevel> create(@RequestBody CourseLevel d) {
        if (service.listLevels(false).size() >= 5) {
            return Result.error(ResultCode.PARAM_ERROR);
        }
        d.setId(null);
        if (d.getSort() == null || d.getSort() < 1 || d.getSort() > 5) {
            d.setSort(1);
        }
        return Result.success(service.saveLevel(d));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<CourseLevel> update(@PathVariable Long id, @RequestBody CourseLevel d) {
        d.setId(id);
        if (d.getSort() != null && (d.getSort() < 1 || d.getSort() > 5)) {
            d.setSort(1);
        }
        return Result.success(service.saveLevel(d));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteLevel(id);
        return Result.success();
    }
}
