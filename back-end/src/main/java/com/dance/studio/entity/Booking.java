package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Booking {
    private Long id;
    private Long storeId;
    private Long userId;
    private Long scheduleId;
    /** booked待上课 checked已签到 cancelled已取消 noshow爽约 waitlist候补中 */
    private String status;
    private Integer lockedHours;
    private Integer waitlistNo;
    private Integer isAdmin;
    private Integer cancelDeduct;
    private LocalDateTime cancelAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
