package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 问答-问题 */
@Data
public class QaQuestion {
    private Long id;
    private Long storeId;
    private Long userId;
    private String content;
    /** JSON数组字符串 */
    private String images;
    private Integer answerCount;
    private Integer likeCount;
    /** 1正常 0后台隐藏 */
    private Integer status;
    private LocalDateTime createdAt;
}
