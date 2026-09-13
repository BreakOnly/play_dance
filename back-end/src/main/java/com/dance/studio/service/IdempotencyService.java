package com.dance.studio.service;

import com.dance.studio.common.exception.BizException;
import com.dance.studio.mapper.OperationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.util.*;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final StudioLock lock;
    private final OperationMapper mapper;
    private final ObjectMapper json;

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(
            String scope, String key, Object payload, Class<T> resultType, Supplier<T> operation) {
        if (key == null || !key.matches("[a-zA-Z0-9_-]{16,80}"))
            throw new BizException("缺少有效操作编号，请刷新后重试");
        lock.acquire();
        try {
            String hash =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(json.writeValueAsBytes(payload)));
            Map<String, Object> prior = mapper.find(scope, key);
            if (prior != null) {
                if (!hash.equals(prior.get("payload_hash")))
                    throw new BizException("相同操作编号不能用于不同内容");
                return json.readValue((String) prior.get("result_json"), resultType);
            }
            T result = operation.get();
            mapper.insert(scope, key, hash, json.writeValueAsString(result));
            return result;
        } catch (BizException e) {
            throw e;
        } catch (java.io.IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
