package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.dto.AdminLoginReq;
import com.dance.studio.entity.Admin;
import com.dance.studio.entity.Store;
import com.dance.studio.service.AuthService;
import com.dance.studio.service.StoreService;
import com.dance.studio.vo.LoginVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "后台-鉴权")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final StoreService storeService;
    private final com.dance.studio.service.ConfigService configService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVo> login(@Valid @RequestBody AdminLoginReq req) {
        return Result.success(authService.adminLogin(req));
    }

    @Operation(summary = "当前管理员信息(含门店名)")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Admin a = authService.adminInfo();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("username", a.getUsername());
        map.put("nickname", a.getNickname() == null ? "" : a.getNickname());
        map.put("role", a.getRole());
        map.put("storeId", a.getStoreId());
        // 门店名（侧边栏/登录态展示用，替换前端写死）
        Store store = storeService.findById(a.getStoreId());
        map.put(
                "storeName",
                configService.getStr(1L, "studio_name", store != null ? store.getName() : "街舞工作室"));
        return Result.success(map);
    }

    @Operation(summary = "登出并撤销现有管理员会话")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> password(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }
}
