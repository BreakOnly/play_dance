package com.dance.studio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointAdjustReq {

    /** 正数增加，负数扣减 */
    @NotNull(message = "请输入积分变动值")
    private Integer delta;

    @NotBlank(message = "请填写原因")
    private String reason;
}
