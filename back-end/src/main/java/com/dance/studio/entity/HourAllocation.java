package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HourAllocation {
    private Long bookingId;
    private Long batchId;
    private Integer hours;
    private String state;
    private LocalDateTime expireAt;
}
