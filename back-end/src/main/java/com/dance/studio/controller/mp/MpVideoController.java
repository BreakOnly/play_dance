package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Video;
import com.dance.studio.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "小程序-课堂视频")
@RestController
@RequestMapping("/api/mp/videos")
@RequiredArgsConstructor
public class MpVideoController {

    private final VideoService videoService;

    @Operation(summary = "我可见的视频")
    @GetMapping
    public Result<List<Video>> list() {
        return Result.success(videoService.listForStudent(ContextHolder.userId()));
    }

    @Operation(summary = "取签名播放地址(鉴权)")
    @GetMapping("/{id}/play")
    public Result<Map<String, String>> play(@PathVariable Long id) {
        String url = videoService.playUrl(id, ContextHolder.userId());
        return Result.success(Map.of("url", url));
    }
}
