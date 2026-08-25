package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.RedeemOrder;
import com.dance.studio.service.RedeemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台-兑换/发放管理")
@RestController
@RequestMapping("/api/admin/redeem-orders")
@RequiredArgsConstructor
public class RedeemController {

    private final RedeemService redeemService;

    @Operation(summary = "发放单列表(按 status/source 过滤)")
    @GetMapping
    public Result<List<RedeemOrder>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String source) {
        return Result.success(redeemService.adminList(ContextHolder.storeId(), status, source));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        redeemService.approve(id, ContextHolder.userId());
        return Result.success();
    }

    @Operation(summary = "驳回(退积分+回库存)")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        redeemService.reject(id, ContextHolder.userId(), reason);
        return Result.success();
    }

    @Operation(summary = "填物流(转配送中)")
    @PutMapping("/{id}/logistics")
    public Result<Void> logistics(@PathVariable Long id, @RequestBody Map<String, String> body) {
        redeemService.ship(id, body.get("company"), body.get("no"));
        return Result.success();
    }

    @Operation(summary = "完成")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        redeemService.complete(id);
        return Result.success();
    }
}
