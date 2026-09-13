package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.service.UserVideoService;
import com.dance.studio.service.storage.StorageService;
import com.dance.studio.vo.UserVideoVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 小程序-视频（管理员上传，用户查看+点赞） */
@Tag(name = "小程序-视频")
@RestController
@RequiredArgsConstructor
public class MpUserVideoController {

    private final UserVideoService userVideoService;
    private final StorageService storageService;

    @Operation(summary = "点赞排行(approved按赞数降序)")
    @GetMapping("/api/mp/videos/ranking")
    public Result<List<UserVideoVo>> ranking(@RequestParam(required = false) Integer limit) {
        return Result.success(
                userVideoService.ranking(ContextHolder.storeId(), ContextHolder.userId(), limit));
    }

    @Operation(summary = "推荐视频(后台推荐)")
    @GetMapping("/api/mp/videos/recommended")
    public Result<List<UserVideoVo>> recommended(@RequestParam(required = false) Integer limit) {
        return Result.success(
                userVideoService.recommended(
                        ContextHolder.storeId(), ContextHolder.userId(), limit));
    }

    @Operation(summary = "视频点赞/取消")
    @PostMapping("/api/mp/videos/{id}/like")
    public Result<Map<String, Object>> like(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        if (body.get("liked") == null)
            throw new com.dance.studio.common.exception.BizException("请提供点赞状态");
        return Result.success(
                userVideoService.toggleLike(id, ContextHolder.userId(), body.get("liked")));
    }

    @Operation(summary = "视频播放地址(签名)")
    @GetMapping("/api/mp/user-videos/{id}/play")
    public Result<Map<String, String>> play(@PathVariable Long id) {
        return Result.success(Map.of("url", userVideoService.playUrl(id)));
    }

    @Operation(summary = "上传图片(封面/问答配图等)")
    @PostMapping("/api/mp/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.upload(file, "image");
        return Result.success(Map.of("url", url));
    }
}
