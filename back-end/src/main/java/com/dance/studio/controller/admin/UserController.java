package com.dance.studio.controller.admin;

import com.dance.studio.common.PageResult;
import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.dto.PointAdjustReq;
import com.dance.studio.dto.RechargeReq;
import com.dance.studio.dto.UserSaveReq;
import com.dance.studio.entity.HourLog;
import com.dance.studio.entity.PointLog;
import com.dance.studio.entity.User;
import com.dance.studio.service.AccountService;
import com.dance.studio.service.UserService;
import com.github.pagehelper.PageHelper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-学员管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AccountService accountService;
    private final com.dance.studio.service.IdempotencyService idempotency;

    @Operation(summary = "学员列表(分页/搜索/状态)")
    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageHelper.startPage(page, size);
        List<User> list = userService.list(keyword, status);
        return Result.success(PageResult.of(list));
    }

    @Operation(summary = "学员详情")
    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @Operation(summary = "编辑学员")
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UserSaveReq req) {
        return Result.success(userService.update(id, req));
    }

    @Operation(summary = "课时充值")
    @PostMapping("/{id}/recharge")
    public Result<User> recharge(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RechargeReq req) {
        return Result.success(
                idempotency.execute(
                        "recharge:" + id,
                        key,
                        req,
                        User.class,
                        () -> {
                            accountService.recharge(id, req, ContextHolder.userId());
                            return userService.detail(id);
                        }));
    }

    @Operation(summary = "积分手动增减")
    @PostMapping("/{id}/points")
    public Result<User> adjustPoints(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody PointAdjustReq req) {
        return Result.success(
                idempotency.execute(
                        "points:" + id,
                        key,
                        req,
                        User.class,
                        () -> {
                            accountService.adjustPoints(
                                    id, req.getDelta(), req.getReason(), ContextHolder.userId());
                            return userService.detail(id);
                        }));
    }

    @Operation(summary = "课时流水")
    @GetMapping("/{id}/hour-logs")
    public Result<List<HourLog>> hourLogs(@PathVariable Long id) {
        return Result.success(accountService.hourLogs(id));
    }

    @Operation(summary = "积分流水")
    @GetMapping("/{id}/point-logs")
    public Result<List<PointLog>> pointLogs(@PathVariable Long id) {
        return Result.success(accountService.pointLogs(id));
    }
}
