package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.service.BookingService;
import com.dance.studio.vo.BookingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台-约课管理")
@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "约课明细：传课次ID按课次查；不传则查全部未结束课次的约课")
    @GetMapping
    public Result<List<BookingVo>> bySchedule(@RequestParam(required = false) Long scheduleId) {
        return Result.success(bookingService.bySchedule(scheduleId));
    }

    @Operation(summary = "代约")
    @PostMapping
    public Result<BookingVo> book(@RequestBody Map<String, Long> body) {
        return Result.success(bookingService.book(body.get("userId"), body.get("scheduleId"), true));
    }

    @Operation(summary = "代取消")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        bookingService.cancel(id, null, true);
        return Result.success();
    }
}
