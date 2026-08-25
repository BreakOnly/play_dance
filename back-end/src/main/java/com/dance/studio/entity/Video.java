package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Video {
    private Long id;
    private Long storeId;
    private Long courseId;
    private Long scheduleId;
    private LocalDate videoDate;
    private String title;
    private String fileUrl;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    /** checked_in已签到 booked已约课 custom指定 */
    private String scopeType;
    private Integer status;
    private LocalDateTime createdAt;
}
