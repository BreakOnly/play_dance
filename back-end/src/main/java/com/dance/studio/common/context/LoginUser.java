package com.dance.studio.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户ID（admin.id 或 user.id） */
    private Long userId;
    /** 门店ID */
    private Long storeId;
    /** 类型：admin / user */
    private String type;

    public static final String TYPE_ADMIN = "admin";
    public static final String TYPE_USER = "user";
}
