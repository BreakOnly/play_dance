package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.service.CheckinCodeService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MpCheckinController {
    private final CheckinCodeService service;

    @PostMapping("/api/mp/checkins/scan")
    public Result<Map<String, Object>> scan(@RequestBody Map<String, String> body) {
        return Result.success(service.checkin(body.get("scene"), ContextHolder.userId()));
    }
}
