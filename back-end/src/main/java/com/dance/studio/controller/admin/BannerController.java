package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Banner;
import com.dance.studio.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-Banner管理")
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "列表(含下架)")
    @GetMapping
    public Result<List<Banner>> list() {
        return Result.success(bannerService.list(ContextHolder.storeId()));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<Banner> create(@RequestBody Banner b) {
        return Result.success(bannerService.save(null, b, ContextHolder.storeId()));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<Banner> update(@PathVariable Long id, @RequestBody Banner b) {
        return Result.success(bannerService.save(id, b, ContextHolder.storeId()));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id, ContextHolder.storeId());
        return Result.success();
    }
}
