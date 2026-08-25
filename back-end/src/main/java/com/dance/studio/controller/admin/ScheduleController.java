package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.dto.ScheduleSaveReq;
import com.dance.studio.service.ScheduleService;
import com.dance.studio.vo.ScheduleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "后台-排课")
@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "排课列表(周视图/筛选)")
    @GetMapping
    public Result<List<ScheduleVo>> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                         @RequestParam(required = false) Long courseId,
                                         @RequestParam(required = false) Long teacherId) {
        return Result.success(scheduleService.list(start, end, courseId, teacherId));
    }

    @Operation(summary = "课次详情")
    @GetMapping("/{id}")
    public Result<ScheduleVo> detail(@PathVariable Long id) {
        return Result.success(scheduleService.detail(id));
    }

    @Operation(summary = "新增排课(含冲突检测)")
    @PostMapping
    public Result<ScheduleVo> create(@Valid @RequestBody ScheduleSaveReq req) {
        return Result.success(scheduleService.save(null, req));
    }

    @Operation(summary = "编辑排课")
    @PutMapping("/{id}")
    public Result<ScheduleVo> update(@PathVariable Long id, @Valid @RequestBody ScheduleSaveReq req) {
        return Result.success(scheduleService.save(id, req));
    }

    @Operation(summary = "取消课次")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        scheduleService.cancel(id);
        return Result.success();
    }
}
