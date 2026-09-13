package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Admin {
    private Integer tokenVersion;
    private Long id;
    private Long storeId;
    private String username;
    private String passwordHash;
    private String nickname;
    private String role;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
