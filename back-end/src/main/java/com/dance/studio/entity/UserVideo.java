package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 学员投稿视频（点赞排行） */
@Data
public class UserVideo {
    private Long id;
    private Long storeId;
    private Long studentId;
    private String title;
    private String fileUrl;
    private String coverUrl;
    private String description;
    /** 秒 */
    private Integer duration;
    private Long fileSize;
    /** pending待审 approved已通过 rejected已驳回 */
    private String status;
    private String rejectReason;
    private Integer isRecommended;
    private Integer likeCount;
    private Long auditAdminId;
    private LocalDateTime auditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
