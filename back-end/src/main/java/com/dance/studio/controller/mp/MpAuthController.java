package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.dto.MpLoginReq;
import com.dance.studio.dto.ProfileUpdateReq;
import com.dance.studio.entity.User;
import com.dance.studio.service.AuthService;
import com.dance.studio.vo.LoginVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "小程序-鉴权与资料")
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpAuthController {

    private final AuthService authService;

    @Operation(summary = "微信登录")
    @PostMapping("/auth/login")
    public Result<LoginVo> login(@Valid @RequestBody MpLoginReq req) {
        return Result.success(authService.mpLogin(req));
    }

    @Operation(summary = "微信手机号授权绑定(企业主体；需登录)")
    @PostMapping("/auth/phone")
    public Result<String> bindPhone(@RequestBody Map<String, String> body) {
        return Result.success(authService.bindPhone(body.get("code")));
    }

    @Operation(summary = "我的资料")
    @GetMapping("/profile")
    public Result<User> profile() {
        return Result.success(authService.mpProfile());
    }

    @Operation(summary = "更新资料")
    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody ProfileUpdateReq req) {
        return Result.success(authService.updateProfile(req));
    }
}
