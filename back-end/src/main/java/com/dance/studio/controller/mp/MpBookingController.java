package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.service.BookingService;
import com.dance.studio.vo.BookingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "小程序-约课")
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpBookingController {

    private final BookingService bookingService;

    @Operation(summary = "约课")
    @PostMapping("/bookings")
    public Result<BookingVo> book(@RequestBody Map<String, Long> body) {
        Long scheduleId = body.get("scheduleId");
        return Result.success(bookingService.book(ContextHolder.userId(), scheduleId, false));
    }

    @Operation(summary = "我的约课(可按状态)")
    @GetMapping("/bookings")
    public Result<List<BookingVo>> my(@RequestParam(required = false) String status) {
        return Result.success(bookingService.myBookings(ContextHolder.userId(), status));
    }

    @Operation(summary = "取消/请假")
    @PostMapping("/bookings/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        bookingService.cancel(id, ContextHolder.userId(), false);
        return Result.success();
    }
}
