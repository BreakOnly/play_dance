package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.entity.Video;
import com.dance.studio.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "后台-视频管理")
@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Data
    public static class VideoSaveReq {
        private Long courseId;
        private Long scheduleId;
        private LocalDate videoDate;
        private String title;
        private String fileUrl;
        private String coverUrl;
        private Integer duration;
        private Long fileSize;
        private String scopeType;
        private List<Long> visibleUserIds;
    }

    @Operation(summary = "列表")
    @GetMapping
    public Result<List<Video>> list(@RequestParam(required = false) Long courseId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(videoService.listAdmin(courseId, date));
    }

    @Operation(summary = "新增视频(上传后传入 fileUrl)")
    @PostMapping
    public Result<Video> create(@RequestBody VideoSaveReq req) {
        Video v = new Video();
        v.setCourseId(req.getCourseId());
        v.setScheduleId(req.getScheduleId());
        v.setVideoDate(req.getVideoDate());
        v.setTitle(req.getTitle());
        v.setFileUrl(req.getFileUrl());
        v.setCoverUrl(req.getCoverUrl());
        v.setDuration(req.getDuration());
        v.setFileSize(req.getFileSize());
        v.setScopeType(req.getScopeType());
        return Result.success(videoService.save(v, req.getVisibleUserIds()));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        videoService.delete(id);
        return Result.success();
    }
}
