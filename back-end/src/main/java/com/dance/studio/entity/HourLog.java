package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HourLog {
    private Long id;
    private Long userId;
    /** recharge/gift/lock/deduct/refund/noshow/expire */
    private String type;
    private Integer amount;
    private Integer balanceAfter;
    private String refType;
    private Long refId;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
