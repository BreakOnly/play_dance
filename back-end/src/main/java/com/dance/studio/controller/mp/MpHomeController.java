package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.User;
import com.dance.studio.mapper.CourseHourBatchMapper;
import com.dance.studio.mapper.UserMapper;
import com.dance.studio.service.AccountService;
import com.dance.studio.service.BannerService;
import com.dance.studio.service.BookingService;
import com.dance.studio.service.ConfigService;
import com.dance.studio.service.NotificationService;
import com.dance.studio.service.UserVideoService;
import com.dance.studio.vo.BookingVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "小程序-首页")
@RestController
@RequestMapping("/api/mp/home")
@RequiredArgsConstructor
public class MpHomeController {

    private final UserMapper userMapper;
    private final CourseHourBatchMapper batchMapper;
    private final BookingService bookingService;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final ConfigService configService;
    private final BannerService bannerService;
    private final UserVideoService userVideoService;

    @Operation(summary = "首页聚合(账户/Banner/体验课/新手指南/视频推荐/今日课/提醒)")
    @GetMapping
    public Result<Map<String, Object>> home() {
        Long sid = ContextHolder.userId();
        Long storeId = ContextHolder.storeId();
        accountService.balances(sid);
        User stu = userMapper.findById(sid);

        int available = (stu.getCourseBalance() == null ? 0 : stu.getCourseBalance());
        int frozen = (stu.getFrozenHours() == null ? 0 : stu.getFrozenHours());

        // 今日已约课
        LocalDate today = LocalDate.now();
        List<BookingVo> todayCourses =
                bookingService.myBookings(sid, "booked").stream()
                        .filter(
                                b ->
                                        b.getStartTime() != null
                                                && b.getStartTime().toLocalDate().isEqual(today))
                        .toList();

        // 提醒
        int lowThreshold = configService.getInt(storeId, "low_hours_threshold", 2);

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("name", stu.getName());
        account.put("avatar", stu.getAvatar());
        account.put("courseAvailable", available);
        account.put("frozenHours", frozen);
        account.put("pointsBalance", stu.getPointsBalance());
        data.put("account", account);
        data.put("banners", bannerService.listEnabled(storeId));
        data.put("todayCourses", todayCourses);
        data.put("unreadCount", notificationService.unread(sid));

        Map<String, Object> reminder = new LinkedHashMap<>();
        reminder.put("lowHours", available <= lowThreshold);
        data.put("reminder", reminder);

        // 新人体验课（config 配置，点击引导联系客服）
        Map<String, Object> trial = new LinkedHashMap<>();
        trial.put("title", configService.getStr(storeId, "trial_title", "新人体验课"));
        trial.put("subtitle", configService.getStr(storeId, "trial_subtitle", ""));
        trial.put("price", configService.getStr(storeId, "trial_price", "9.9"));
        trial.put("originPrice", configService.getStr(storeId, "trial_origin_price", "199"));
        data.put("trial", trial);

        // 客服电话（体验课/充值共用）
        data.put("servicePhone", configService.getStr(storeId, "service_phone", ""));

        // 新手指南 3 步：购买体验课(充值过) -> 选择门店课程(有预约) -> 到店试课(已签到)
        List<BookingVo> allBookings = bookingService.myBookings(sid, null);
        boolean hasBooking = !allBookings.isEmpty();
        boolean hasChecked = allBookings.stream().anyMatch(b -> "checked".equals(b.getStatus()));
        boolean hasRecharged =
                accountService.hourLogs(sid).stream()
                        .anyMatch(
                                l -> "recharge".equals(l.getType()) || "gift".equals(l.getType()));
        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("step1Done", hasRecharged);
        guide.put("step2Done", hasBooking);
        guide.put("step3Done", hasChecked);
        data.put("guide", guide);

        // 视频推荐（后台推荐位）
        data.put("videos", userVideoService.recommended(storeId, sid, 6));

        // 会员有效期（最早到期有效批次）
        LocalDateTime expireAt = batchMapper.earliestExpire(sid);
        if (expireAt != null) {
            data.put("membershipExpireAt", expireAt.toLocalDate().toString());
            data.put(
                    "membershipRemainDays",
                    ChronoUnit.DAYS.between(LocalDate.now(), expireAt.toLocalDate()));
        } else {
            data.put("membershipExpireAt", "");
            data.put("membershipRemainDays", null);
        }

        return Result.success(data);
    }
}
