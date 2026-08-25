package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseSchedule {
    private Long id;
    private Long storeId;
    private Long courseId;
    private Long teacherId;
    private String room;
    /** 本课次曲目 */
    private String song;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    /** open报名中 full满员 finished已结束 cancelled已取消 */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
