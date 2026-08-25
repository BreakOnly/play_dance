package com.dance.studio.service;

import com.dance.studio.entity.Config;
import com.dance.studio.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务规则配置读取（带简单本地缓存）。后续约课/签到/积分等阶段统一从这里取阈值。
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigMapper configMapper;

    public List<Config> list(Long storeId) {
        return configMapper.listByStore(storeId);
    }

    public String getStr(Long storeId, String key, String def) {
        Config c = configMapper.findByKey(storeId, key);
        return c != null ? c.getConfigValue() : def;
    }

    public int getInt(Long storeId, String key, int def) {
        String v = getStr(storeId, key, null);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    public boolean getBool(Long storeId, String key, boolean def) {
        String v = getStr(storeId, key, null);
        if (v == null) return def;
        return "1".equals(v.trim()) || "true".equalsIgnoreCase(v.trim());
    }

    /** 批量更新（仅更新已存在的 key） */
    public void update(Long storeId, java.util.Map<String, String> kv) {
        kv.forEach((k, v) -> configMapper.update(storeId, k, v));
    }
}
