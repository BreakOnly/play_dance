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
        map.put("storeName", store != null ? store.getName() : "");
        return Result.success(map);
    }

    @Operation(summary = "登出(前端清 token 即可)")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
