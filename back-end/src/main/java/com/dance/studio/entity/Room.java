package com.dance.studio.entity;

import lombok.Data;

@Data
public class Room {
    private Long id;
    private Long storeId;
    /** 教室名 */
    private String name;
    /** 最大容纳人数(0=不限制) */
    private Integer maxCapacity;
    private Integer sort;
    private Integer status;
}
