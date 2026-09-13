package com.dance.studio.service;

import com.dance.studio.entity.Config;
import com.dance.studio.mapper.ConfigMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/** 业务配置统一读取；更新按类型校验、原子保存并记录操作者。 */
@Service
@RequiredArgsConstructor
public class ConfigService {
    private final StudioLock lock;
    private final com.dance.studio.mapper.ConfigurationChangeMapper changes;
    private static final java.util.Map<String, int[]> LIMITS =
            java.util.Map.ofEntries(
                    java.util.Map.entry("default_capacity_group", new int[] {1, 200}),
                    java.util.Map.entry("cancel_free_hours", new int[] {0, 168}),
                    java.util.Map.entry("book_ahead_days", new int[] {1, 90}),
                    java.util.Map.entry("book_cutoff_min", new int[] {0, 1440}),
                    java.util.Map.entry("checkin_before_min", new int[] {0, 120}),
                    java.util.Map.entry("course_validity_days", new int[] {0, 36500}),
                    java.util.Map.entry("points_per_checkin", new int[] {0, 10000}),
                    java.util.Map.entry("points_per_yuan", new int[] {0, 100}),
                    java.util.Map.entry("waitlist_limit", new int[] {0, 50}),
                    java.util.Map.entry("low_hours_threshold", new int[] {0, 100}),
                    java.util.Map.entry("expiry_reminder_days", new int[] {1, 365}),
                    java.util.Map.entry("redeem_auto_approve", new int[] {0, 1}));
    private static final java.util.Set<String> TEXT =
            java.util.Set.of(
                    "trial_title",
                    "trial_subtitle",
                    "service_phone",
                    "course_notice",
                    "studio_name",
                    "privacy_url",
                    "terms_url");

    private final ConfigMapper configMapper;

    public List<Config> list(Long storeId) {
        return configMapper.listByStore(storeId).stream()
                .filter(
                        c ->
                                LIMITS.containsKey(c.getConfigKey())
                                        || TEXT.contains(c.getConfigKey())
                                        || c.getConfigKey().equals("trial_price")
                                        || c.getConfigKey().equals("trial_origin_price"))
                .toList();
    }

    public String getStr(Long storeId, String key, String def) {
        Config c = configMapper.findByKey(storeId, key);
        return c != null ? c.getConfigValue() : def;
    }

    public int getInt(Long storeId, String key, int def) {
        String v = getStr(storeId, key, null);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBool(Long storeId, String key, boolean def) {
        String v = getStr(storeId, key, null);
        if (v == null) return def;
        return "1".equals(v.trim()) || "true".equalsIgnoreCase(v.trim());
    }

    /** 批量更新（仅更新已存在的 key） */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void update(Long storeId, java.util.Map<String, String> kv) {
        lock.acquire();
        kv.forEach(
                (k, v) -> {
                    if (v == null || v.length() > 255)
                        throw new com.dance.studio.common.exception.BizException("配置长度不正确：" + k);
                    try {
                        if (LIMITS.containsKey(k)) {
                            int n = Integer.parseInt(v);
                            int[] range = LIMITS.get(k);
                            if (n < range[0] || n > range[1]) throw new IllegalArgumentException();
                        } else if (k.equals("trial_price") || k.equals("trial_origin_price")) {
                            var n = new java.math.BigDecimal(v);
                            if (n.signum() < 0
                                    || n.scale() > 2
                                    || n.compareTo(new java.math.BigDecimal("1000000")) > 0)
                                throw new IllegalArgumentException();
                        } else if (!TEXT.contains(k)) throw new IllegalArgumentException();
                        if ((k.equals("privacy_url") || k.equals("terms_url"))
                                && !v.isBlank()
                                && !v.startsWith("https://")) throw new IllegalArgumentException();
                    } catch (Exception e) {
                        throw new com.dance.studio.common.exception.BizException("配置值或配置键不正确：" + k);
                    }
                });
        kv.forEach(
                (k, v) -> {
                    String old = getStr(storeId, k, "");
                    if (!old.equals(v)) {
                        changes.insert(
                                com.dance.studio.common.context.ContextHolder.userId(), k, old, v);
                        configMapper.update(storeId, k, v);
                    }
                });
    }
}
