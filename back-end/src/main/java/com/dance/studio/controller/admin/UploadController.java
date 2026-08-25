package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.service.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "后台-文件上传")
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @Operation(summary = "上传图片")
    @PostMapping
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.upload(file, "image");
        return Result.success(Map.of("url", url));
    }

    @Operation(summary = "上传视频")
    @PostMapping("/video")
    public Result<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        String url = storageService.upload(file, "video");
        return Result.success(Map.of("url", url));
    }
}
