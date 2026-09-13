package com.dance.studio.service;

import com.dance.studio.common.exception.BizException;
import com.dance.studio.mapper.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckinCodeService {
    private final CheckinCodeMapper mapper;
    private final CourseScheduleMapper schedules;
    private final CheckinService checkins;
    private final WechatService wechat;
    private final ConfigService config;
    private final SecureRandom random = new SecureRandom();

    public Map<String, Object> create(Long scheduleId) {
        var s = schedules.findById(scheduleId);
        LocalDateTime now = LocalDateTime.now();
        if (s == null
                || !Long.valueOf(1).equals(s.getStoreId())
                || "cancelled".equals(s.getStatus())
                || !s.getEndTime().isAfter(now)) throw new BizException("课程不可签到");
        if (now.isBefore(
                s.getStartTime().minusMinutes(config.getInt(1L, "checkin_before_min", 30))))
            throw new BizException("请在签到时段内生成签到码");
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        LocalDateTime expiry = now.plusMinutes(5);
        if (expiry.isAfter(s.getEndTime())) expiry = s.getEndTime();
        byte[] image = wechat.unlimitedCode(token);
        mapper.cleanup();
        mapper.insert(hash(token), scheduleId, expiry);
        return Map.of(
                "image",
                "data:"
                        + (image[0] == (byte) 0x89 ? "image/png" : "image/jpeg")
                        + ";base64,"
                        + Base64.getEncoder().encodeToString(image),
                "expiresAt",
                expiry,
                "validForSeconds",
                java.time.Duration.between(now, expiry).getSeconds());
    }

    public Map<String, Object> checkin(String scene, Long userId) {
        if (scene == null || !scene.matches("[0-9a-f]{32}"))
            throw new BizException("签到码无效，请使用微信扫一扫门店签到码");
        Long scheduleId = mapper.resolve(hash(scene));
        if (scheduleId == null) throw new BizException("签到码已失效，请扫描最新签到码");
        return checkins.scanCheckin(scheduleId, userId);
    }

    private String hash(String s) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
