package com.dance.studio.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Course {
    private Long id;
    private Long storeId;
    private String name;
    /** group团课 / private私教 */
    private String type;
    private Long danceTypeId;
    private Long levelId;
    private Integer durationMin;
    private BigDecimal price;
    private Integer costHours;
    private String cover;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
