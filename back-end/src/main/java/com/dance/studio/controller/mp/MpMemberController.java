package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.User;
import com.dance.studio.mapper.BookingMapper;
import com.dance.studio.mapper.CourseHourBatchMapper;
import com.dance.studio.mapper.UserMapper;
import com.dance.studio.service.BookingService;
import com.dance.studio.vo.BookingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    @Operation(summary = "会员资料（姓名/等级/课时/冻结/有效期/临期/待上课数）")
    @GetMapping
    public Result<Map<String, Object>> member() {
        Long sid = ContextHolder.userId();
        User stu = userMapper.findById(sid);

        int available = stu.getCourseBalance() == null ? 0 : stu.getCourseBalance();
        int frozen = stu.getFrozenHours() == null ? 0 : stu.getFrozenHours();
        int totalHours = available + frozen;

        // 待上课数
        List<BookingVo> booked = bookingService.myBookings(sid, "booked");
        int upcomingCount = booked.size();

        // 有效期
        LocalDateTime expireAt = batchMapper.earliestExpire(sid);
        String validUntil = expireAt != null ? expireAt.toLocalDate().toString() : "长期有效";

        // 临期课时（7天内到期）
        int expiringHours = 0;
        if (expireAt != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), expireAt.toLocalDate());
            if (days >= 0 && days <= 7) {
                expiringHours = available;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", stu.getName());
        data.put("phone", stu.getPhone());
        data.put("totalHours", totalHours);
        data.put("frozenHours", frozen);
        data.put("availableHours", available);
        data.put("upcomingCount", upcomingCount);
        data.put("validUntil", validUntil);
        data.put("expiringHours", expiringHours);

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