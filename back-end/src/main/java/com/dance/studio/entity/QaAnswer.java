package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 问答-回答 */
@Data
public class QaAnswer {
    private Long id;
    private Long questionId;
    private Long studentId;
    private Long teacherId;
    private Integer isTeacher;
    private String content;
    private Integer likeCount;
    /** 1正常 0后台隐藏 */
    private Integer status;
    private LocalDateTime createdAt;
}
