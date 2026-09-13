package com.dance.studio.service;

import com.dance.studio.mapper.StudioLockMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class StudioLock {
    private final StudioLockMapper mapper;

    public void acquire() {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("门店写操作必须在事务中执行");
        if (mapper.lock() == null) throw new IllegalStateException("请先初始化门店 1");
    }
}
