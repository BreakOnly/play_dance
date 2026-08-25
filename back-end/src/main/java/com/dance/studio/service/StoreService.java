package com.dance.studio.service;

import com.dance.studio.entity.Store;
import com.dance.studio.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 门店信息服务（多门店预留，当前固定门店 1）。
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreMapper storeMapper;

    /** 查门店，不存在返回 null */
    public Store findById(Long id) {
        return id == null ? null : storeMapper.findById(id);
    }
}
