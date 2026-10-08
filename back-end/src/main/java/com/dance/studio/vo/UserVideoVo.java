package com.dance.studio.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 投稿视频（含作者信息与当前用户点赞态） */
@Data
public class UserVideoVo {
    private Long id;
    private Long userId;
    private String title;
    private String coverUrl;
    /** 存储地址（仅后台列表返回，小程序端接口不返回） */
    private String fileUrl;
    private String description;
    /** 秒 */
    private Integer duration;
    private String status;
    private String rejectReason;
    private Integer isRecommended;
    private Integer likeCount;
    private String authorName;
    private String authorAvatar;
    /** 当前用户是否已点赞 */
    private Boolean liked;
    private LocalDateTime createdAt;
}
