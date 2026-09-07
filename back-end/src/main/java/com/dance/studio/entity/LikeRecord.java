package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 点赞记录（question/answer/video 统一去重） */
@Data
public class LikeRecord {
    private Long id;
    private Long userId;
    /** question/answer/video */
    private String targetType;
    private Long targetId;
    private LocalDateTime createdAt;
}
