package com.dance.studio.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RechargeOrder {
    private Long id;
    private Long storeId;
    private Long studentId;
    private Integer hours;
    private Integer giftHours;
    private BigDecimal amount;
    private Integer pointsGain;
    private Integer validityDays;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
