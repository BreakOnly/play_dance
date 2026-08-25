package com.dance.studio.entity;

import lombok.Data;

@Data
public class CourseLevel {
    private Long id;
    private Long storeId;
    private String name;
    private Integer sort;
    private Integer status;
}
