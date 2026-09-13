package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 注意事项（排课时选填关联到课次，小程序课程详情展示）
 */
@Data
public class Notice {
    private Long id;
    private Long storeId;
    /** 注意事项名称（排课下拉选择用） */
    private String name;
    /** 内容（支持换行） */
    private String content;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
