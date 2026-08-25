package com.dance.studio.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVo {
    private String token;
    private Long userId;
    private String name;
}
