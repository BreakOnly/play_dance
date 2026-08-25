package com.dance.studio.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程列表/详情（含舞种、难度名称，便于前端直接展示）
 */
@Data
public class CourseVo {
    private Long id;
    private String name;
    private String type;
    private Long danceTypeId;
    private String danceTypeName;
    private Long levelId;
    private String levelName;
    private Integer durationMin;
    private BigDecimal price;
    private Integer costHours;
    private String cover;
    private String description;
    private Integer status;
}
