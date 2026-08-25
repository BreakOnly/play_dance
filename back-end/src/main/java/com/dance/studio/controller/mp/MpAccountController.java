package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.HourLog;
import com.dance.studio.entity.PointLog;
import com.dance.studio.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小程序-账户流水")
@RestController
@RequestMapping("/api/mp/account")
@RequiredArgsConstructor
public class MpAccountController {

    private final AccountService accountService;

    @Operation(summary = "课时流水")
    @GetMapping("/hour-logs")
    public Result<List<HourLog>> hourLogs() {
        return Result.success(accountService.hourLogs(ContextHolder.userId()));
    }

    @Operation(summary = "积分流水")
    @GetMapping("/point-logs")
    public Result<List<PointLog>> pointLogs() {
        return Result.success(accountService.pointLogs(ContextHolder.userId()));
    }
}
