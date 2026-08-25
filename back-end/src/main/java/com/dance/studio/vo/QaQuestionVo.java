package com.dance.studio.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 问答-问题（含作者、点赞态、回答预览） */
@Data
public class QaQuestionVo {
    private Long id;
    private Long studentId;
    private String content;
    /** 配图URL数组 */
    private List<String> images;
    private Integer answerCount;
    private Integer likeCount;
    private Boolean liked;
    private String authorName;
    private String authorAvatar;
    /** 1正常 0隐藏（后台管理用） */
    private Integer status;
    private LocalDateTime createdAt;
    /** 首条回答预览（列表用） */
    private QaAnswerVo firstAnswer;
}
