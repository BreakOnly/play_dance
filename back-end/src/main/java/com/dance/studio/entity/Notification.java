package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private Long userId;
    /** booking/checkin/redeem/remind/system */
    private String type;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
