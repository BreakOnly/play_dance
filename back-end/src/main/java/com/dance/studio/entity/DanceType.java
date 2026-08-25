package com.dance.studio.entity;

import lombok.Data;

@Data
public class DanceType {
    private Long id;
    private Long storeId;
    private String name;
    /** 舞种介绍 */
    private String intro;
    private Integer sort;
    private Integer status;
}
