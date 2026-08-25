package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页 Banner（后台可配置，小程序首页顶部轮播）
 */
@Data
public class Banner {
    private Long id;
    private Long storeId;
    /** 主标题 */
    private String title;
    /** 副标题 */
    private String subtitle;
    /** 图片 URL */
    private String imageUrl;
    /** 跳转类型 none/tab/url */
    private String linkType;
    /** 跳转目标（tab 路径或外部链接） */
    private String linkValue;
    /** 排序（小在前） */
    private Integer sort;
    /** 状态 1上架 0下架 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
