package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Config {
    private Long id;
    private Long storeId;
    private String configKey;
    private String configValue;
    private String remark;
    private LocalDateTime updatedAt;
}
