package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Store {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String logo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
