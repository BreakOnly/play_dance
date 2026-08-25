package com.dance.studio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeReq {

    @NotNull(message = "请输入充值课时")
    @Min(value = 1, message = "充值课时需≥1")
    private Integer hours;

    @Min(value = 0, message = "赠送课时不能为负")
    private Integer giftHours = 0;

    private BigDecimal amount = BigDecimal.ZERO;

    /** 本单有效期天数；null=用系统默认；0=永久 */
    private Integer validityDays;

    private String remark;
}
