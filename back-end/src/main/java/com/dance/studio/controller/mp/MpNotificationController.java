package com.dance.studio.controller.mp;

import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.entity.Notification;
import com.dance.studio.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "小程序-消息通知")
@RestController
@RequestMapping("/api/mp/notifications")
@RequiredArgsConstructor
public class MpNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "消息列表")
    @GetMapping
    public Result<List<Notification>> list() {
        return Result.success(notificationService.list(ContextHolder.userId()));
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread")
    public Result<Map<String, Integer>> unread() {
        return Result.success(Map.of("count", notificationService.unread(ContextHolder.userId())));
    }

    @Operation(summary = "标记已读(不传id则全部已读)")
    @PutMapping("/read")
    public Result<Void> read(@RequestBody(required = false) Map<String, Long> body) {
        Long id = body != null ? body.get("id") : null;
        if (id != null) notificationService.read(ContextHolder.userId(), id);
        else notificationService.readAll(ContextHolder.userId());
        return Result.success();
    }
}
