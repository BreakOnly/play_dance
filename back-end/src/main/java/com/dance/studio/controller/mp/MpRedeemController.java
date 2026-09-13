package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Goods;
import com.dance.studio.entity.RedeemOrder;
import com.dance.studio.service.GoodsService;
import com.dance.studio.service.RedeemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "小程序-积分乐园")
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpRedeemController {

    private final GoodsService goodsService;
    private final RedeemService redeemService;
    private final com.dance.studio.service.IdempotencyService idempotency;

    @Operation(summary = "手办列表(?category=claw/redeem)")
    @GetMapping("/goods")
    public Result<List<Goods>> goods(@RequestParam(required = false) String category) {
        return Result.success(goodsService.list(category, 1));
    }

    @Operation(summary = "手办详情")
    @GetMapping("/goods/{id}")
    public Result<Goods> goodsDetail(@PathVariable Long id) {
        return Result.success(goodsService.detail(id));
    }

    @Operation(summary = "指定兑换")
    @PostMapping("/redeem")
    public Result<RedeemOrder> redeem(
            @RequestHeader("Idempotency-Key") String key, @RequestBody Map<String, Long> body) {
        return Result.success(
                idempotency.execute(
                        "redeem:" + ContextHolder.userId(),
                        key,
                        body,
                        RedeemOrder.class,
                        () -> redeemService.redeem(ContextHolder.userId(), body.get("goodsId"))));
    }

    @Operation(summary = "我的兑换/发放记录")
    @GetMapping("/redeem-orders")
    public Result<List<RedeemOrder>> myOrders(@RequestParam(required = false) String status) {
        return Result.success(redeemService.myOrders(ContextHolder.userId(), status));
    }

    @Operation(summary = "我的手办(非驳回)")
    @GetMapping("/my-goods")
    public Result<List<RedeemOrder>> myGoods() {
        return Result.success(
                redeemService.myOrders(ContextHolder.userId(), null).stream()
                        .filter(o -> !"rejected".equals(o.getStatus()))
                        .toList());
    }
}
