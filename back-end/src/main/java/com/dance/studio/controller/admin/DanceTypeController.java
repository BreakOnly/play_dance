package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.entity.DanceType;
import com.dance.studio.service.BasicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-舞种")
@RestController
@RequestMapping("/api/admin/dance-types")
@RequiredArgsConstructor
public class DanceTypeController {

    private final BasicDataService service;

    @Operation(summary = "列表")
    @GetMapping
    public Result<List<DanceType>> list() {
        return Result.success(service.listDanceTypes(false));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<DanceType> create(@RequestBody DanceType d) {
        d.setId(null);
        return Result.success(service.saveDanceType(d));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<DanceType> update(@PathVariable Long id, @RequestBody DanceType d) {
        d.setId(id);
        return Result.success(service.saveDanceType(d));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteDanceType(id);
        return Result.success();
    }
}
