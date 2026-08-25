package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseHourBatch {
    private Long id;
    private Long studentId;
    private Integer total;
    private Integer remaining;
    /** recharge充值 / gift赠送 */
    private String source;
    private Long orderId;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
