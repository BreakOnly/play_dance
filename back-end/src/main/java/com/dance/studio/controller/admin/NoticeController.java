package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.entity.Notice;
import com.dance.studio.service.BasicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台-注意事项管理：排课时选填关联到课次，小程序课程详情展示。
 */
@Tag(name = "后台-注意事项")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final BasicDataService service;

    @Operation(summary = "列表(可只看启用)")
    @GetMapping
    public Result<List<Notice>> list(@RequestParam(required = false, defaultValue = "false") boolean onlyEnabled) {
        return Result.success(service.listNotices(onlyEnabled));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<Notice> create(@RequestBody Notice n) {
        n.setId(null);
        return Result.success(service.saveNotice(n));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<Notice> update(@PathVariable Long id, @RequestBody Notice n) {
        n.setId(id);
        return Result.success(service.saveNotice(n));
    }

    @Operation(summary = "删除(被排课引用时禁止)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteNotice(id);
        return Result.success();
    }
}
