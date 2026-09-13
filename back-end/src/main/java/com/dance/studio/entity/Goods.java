package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Goods {
    private Integer version;
    private Long id;
    private Long storeId;
    private String name;

    /** 多角度图 JSON 数组字符串 */
    private String images;

    private String description;

    /** claw可抓取款 / redeem可兑换款 */
    private String category;

    private Integer pointsRequired;
    private Integer stock;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
