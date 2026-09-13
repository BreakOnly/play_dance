package com.dance.studio.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class AdminLoginReq {

    @NotBlank(message = "请输入账号")
    @jakarta.validation.constraints.Size(max = 50)
    private String username;

    @NotBlank(message = "请输入密码")
    @jakarta.validation.constraints.Size(max = 128)
    private String password;
}
