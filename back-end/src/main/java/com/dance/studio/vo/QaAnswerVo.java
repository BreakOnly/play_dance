package com.dance.studio.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 问答-回答 */
@Data
public class QaAnswerVo {
    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    private Integer isTeacher;
    private Integer likeCount;
    private Boolean liked;
    private String authorName;
    private String authorAvatar;
    private String teacherName;
    /** 1正常 0隐藏（后台管理用） */
    private Integer status;
    private LocalDateTime createdAt;
}
