package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Teacher {
    private Long id;
    private Long storeId;
    private String name;
    private String phone;
    private String avatar;
    private String intro;
    private Integer status;
    private LocalDateTime createdAt;
}
