package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.service.ScheduleService;
import com.dance.studio.vo.ScheduleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "小程序-课表")
@RestController
@RequestMapping("/api/mp/schedules")
@RequiredArgsConstructor
public class MpScheduleController {

    private final ScheduleService scheduleService;
    private final CourseScheduleMapper scheduleMapper;

    @Operation(summary = "课表(跨门店,支持关键字与舞种/难度/门店筛选)")
    @GetMapping
    public Result<List<ScheduleVo>> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long danceTypeId,
                                         @RequestParam(required = false) Long levelId,
                                         @RequestParam(required = false) Long storeId) {
        return Result.success(scheduleMapper.listForMp(start, end, keyword, danceTypeId, levelId,
                storeId, ContextHolder.userId()));
    }

    @Operation(summary = "课次详情")
    @GetMapping("/{id}")
    public Result<ScheduleVo> detail(@PathVariable Long id) {
        return Result.success(scheduleService.detailForStudent(id, ContextHolder.userId()));
    }
}
