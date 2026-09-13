package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Store;
import com.dance.studio.mapper.StoreMapper;
import com.dance.studio.service.ConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 小程序端业务配置（取消规则等），从 config 表读取，前端不写死。 */
@Tag(name = "小程序-配置")
@RestController
@RequestMapping("/api/mp/config")
@RequiredArgsConstructor
public class MpConfigController {

    private final ConfigService configService;
    private final StoreMapper storeMapper;

    @Operation(summary = "获取业务规则(取消政策等)与门店信息")
    @GetMapping
    public Result<Map<String, Object>> getConfig() {
        Long storeId = ContextHolder.storeId();
        Map<String, Object> map = new LinkedHashMap<>();
        // 取消免费时限（小时）
        map.put("cancelFreeHours", configService.getInt(storeId, "cancel_free_hours", 6));
        // 约课提前天数
        map.put("bookAheadDays", configService.getInt(storeId, "book_ahead_days", 7));
        // 开课前停止约课分钟数
        map.put("bookCutoffMin", configService.getInt(storeId, "book_cutoff_min", 30));
        // 签到积分（详情页提示文案）
        map.put("pointsPerCheckin", configService.getInt(storeId, "points_per_checkin", 100));
        // 课程注意事项（详情页）
        map.put("courseNotice", configService.getStr(storeId, "course_notice", ""));
        // 客服电话（充值/体验课弹层）
        map.put("servicePhone", configService.getStr(storeId, "service_phone", ""));
        // 门店信息（替换前端写死的店名/地址）
        Store store = storeMapper.findById(storeId);
        map.put("storeName", store != null ? store.getName() : "");
        map.put("storeAddress", store != null ? store.getAddress() : "");
        map.put("storePhone", store != null ? store.getPhone() : "");
        map.put("checkinBeforeMin", configService.getInt(1L, "checkin_before_min", 30));
        map.put("lockOnBooking", true);
        map.put("waitlistLocksHours", false);
        return Result.success(map);
    }

    @Operation(summary = "启用门店列表(课表筛选)")
    @GetMapping("/stores")
    public Result<List<Store>> stores() {
        return Result.success(storeMapper.listEnabled());
    }
}
