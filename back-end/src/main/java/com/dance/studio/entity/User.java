package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private Long storeId;
    private String openid;
    private String unionid;
    private String name;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private String avatar;
    private Integer courseBalance;
    private Integer pointsBalance;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
