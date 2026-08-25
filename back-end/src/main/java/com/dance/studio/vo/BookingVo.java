package com.dance.studio.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 约课记录（含课次信息，供"我的约课"与后台约课管理展示）
 */
@Data
public class BookingVo {
    private Long id;
    private Long scheduleId;
    private Long studentId;
    private String studentName;
    private String courseName;
    private String courseType;
    private String teacherName;
    private String room;
    /** 门店名（我的约课卡片展示） */
    private String storeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer waitlistNo;
    private Integer lockedHours;
    private LocalDateTime createdAt;
}
