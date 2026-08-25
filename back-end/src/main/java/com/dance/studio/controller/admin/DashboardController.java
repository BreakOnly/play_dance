package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.mapper.DashboardMapper;
import com.dance.studio.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "后台-经营看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardMapper dashboardMapper;
    private final ConfigService configService;

    @Operation(summary = "经营概览(营收/课时/出勤/活跃)")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(dashboardMapper.overview(ContextHolder.storeId(), from, to));
    }

    @Operation(summary = "预警(课时不足/流失)")
    @GetMapping("/alerts")
    public Result<Map<String, Object>> alerts(@RequestParam(defaultValue = "14") int inactiveDays) {
        Long storeId = ContextHolder.storeId();
        int lowThreshold = configService.getInt(storeId, "low_hours_threshold", 2);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lowHours", dashboardMapper.lowHoursStudents(storeId, lowThreshold));
        data.put("inactive", dashboardMapper.inactiveStudents(storeId, LocalDateTime.now().minusDays(inactiveDays)));
        return Result.success(data);
    }
}
