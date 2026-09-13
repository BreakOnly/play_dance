package com.dance.studio.controller;

import com.dance.studio.common.Result;
import com.dance.studio.service.ConfigService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicStudioController {
    private final ConfigService config;

    @GetMapping("/api/public/studio")
    public Result<Map<String, String>> info() {
        return Result.success(
                Map.of(
                        "name",
                        config.getStr(1L, "studio_name", "街舞工作室"),
                        "phone",
                        config.getStr(1L, "service_phone", ""),
                        "privacyUrl",
                        config.getStr(1L, "privacy_url", ""),
                        "termsUrl",
                        config.getStr(1L, "terms_url", "")));
    }
}
