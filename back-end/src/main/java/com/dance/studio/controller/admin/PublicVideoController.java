package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.UserVideo;
import com.dance.studio.service.UserVideoService;
import com.dance.studio.vo.UserVideoVo;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/public-videos")
public class PublicVideoController {
    private final UserVideoService service;

    @GetMapping
    public Result<List<UserVideoVo>> list() {
        return Result.success(service.listAdmin(1L, "approved"));
    }

    @PostMapping
    public Result<UserVideo> publish(@RequestBody UserVideo video) {
        return Result.success(service.publish(video, ContextHolder.userId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> hide(@PathVariable Long id) {
        service.hidePublished(id);
        return Result.success();
    }
}
