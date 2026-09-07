package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Checkin {
    private Long id;
    private Long storeId;
    private Long bookingId;
    private Long userId;
    private Long scheduleId;
    private LocalDateTime checkTime;
    /** scan扫码 manual代签 */
    private String method;
    private Long operatorId;
}
