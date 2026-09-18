package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.User;
import com.dance.studio.mapper.CourseHourBatchMapper;
import com.dance.studio.mapper.UserMapper;
import com.dance.studio.service.BookingService;
import com.dance.studio.vo.BookingVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "小程序-会员")
@RestController
@RequestMapping("/api/mp/member")
@RequiredArgsConstructor
public class MpMemberController {

    private final UserMapper userMapper;
    private final CourseHourBatchMapper batchMapper;
    private final BookingService bookingService;
    private final com.dance.studio.service.AccountService accountService;

    @Operation(summary = "会员资料（姓名/等级/课时/有效期/临期/待上课数）")
    @GetMapping
    public Result<Map<String, Object>> member() {
        Long sid = ContextHolder.userId();
        var balances = accountService.balances(sid);
        User stu = userMapper.findById(sid);

        int available = stu.getCourseBalance() == null ? 0 : stu.getCourseBalance();
        int totalHours = available;

        // 待上课数
        List<BookingVo> booked = bookingService.myBookings(sid, "booked");
        int upcomingCount = booked.size();

        // 有效期
        LocalDateTime expireAt = batchMapper.earliestExpire(sid);
        String validUntil = expireAt != null ? expireAt.toLocalDate().toString() : "长期有效";
        if (totalHours == 0) validUntil = "暂无课时";

        // 按配置天数逐批次汇总临期课时。
        int expiringHours = balances.get("expiring");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", stu.getName());
        data.put("phone", stu.getPhone());
        data.put("totalHours", totalHours);
        data.put("availableHours", available);
        data.put("upcomingCount", upcomingCount);
        data.put("validUntil", validUntil);
        data.put("expiringHours", expiringHours);
        data.put("pointsBalance", balances.get("points"));

        return Result.success(data);
    }

    @Operation(summary = "修改会员姓名")
    @PutMapping
    @Transactional
    public Result<Void> updateName(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR);
        }
        User stu = new User();
        stu.setId(ContextHolder.userId());
        stu.setName(name.trim());
        userMapper.updateProfile(stu);
        return Result.success();
    }
}
