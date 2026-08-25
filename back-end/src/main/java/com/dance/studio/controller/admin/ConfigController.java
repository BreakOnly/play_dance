package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Config;
import com.dance.studio.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台-系统设置")
@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "业务规则列表")
    @GetMapping
    public Result<List<Config>> list() {
        return Result.success(configService.list(ContextHolder.storeId()));
    }

    @Operation(summary = "批量更新业务规则")
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, String> kv) {
        configService.update(ContextHolder.storeId(), kv);
        return Result.success();
    }
}
