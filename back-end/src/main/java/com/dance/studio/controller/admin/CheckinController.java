package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.mapper.CheckinMapper;
import com.dance.studio.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "后台-签到管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final CheckinMapper checkinMapper;

    @Operation(summary = "签到记录")
    @GetMapping("/checkins")
    public Result<List<Map<String, Object>>> records(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                     @RequestParam(required = false) Long scheduleId) {
        return Result.success(checkinMapper.listRecords(ContextHolder.storeId(), from, to, scheduleId));
    }

    @Operation(summary = "管理员代签")
    @PostMapping("/checkins")
    public Result<Map<String, Object>> manual(@RequestBody Map<String, Long> body) {
        return Result.success(checkinService.manualCheckin(body.get("bookingId"), ContextHolder.userId()));
    }

    @Operation(summary = "出勤统计")
    @GetMapping("/reports/attendance")
    public Result<Map<String, Object>> attendance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(checkinMapper.attendanceStat(ContextHolder.storeId(), from, to));
    }
}
