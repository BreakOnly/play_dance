package com.dance.studio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MpLoginReq {

    /** wx.login 返回的 code（开发 mock 模式下直接当 openid） */
    @NotBlank(message = "缺少 code")
    private String code;
}
