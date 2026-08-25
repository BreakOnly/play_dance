package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.dto.GoodsSaveReq;
import com.dance.studio.entity.Goods;
import com.dance.studio.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-手办管理")
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "列表(可按 category=claw/redeem 过滤)")
    @GetMapping
    public Result<List<Goods>> list(@RequestParam(required = false) String category,
                                    @RequestParam(required = false) Integer status) {
        return Result.success(goodsService.list(category, status));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.success(goodsService.detail(id));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<Goods> create(@RequestBody GoodsSaveReq req) {
        return Result.success(goodsService.save(null, req));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<Goods> update(@PathVariable Long id, @RequestBody GoodsSaveReq req) {
        return Result.success(goodsService.save(id, req));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return Result.success();
    }
}
