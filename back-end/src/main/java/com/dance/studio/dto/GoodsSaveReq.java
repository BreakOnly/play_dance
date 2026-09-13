package com.dance.studio.dto;

import lombok.Data;

import java.util.List;

@Data
public class GoodsSaveReq {
    private Integer version;
    private String name;
    private List<String> images;
    private String description;

    /** claw / redeem */
    private String category;

    private Integer pointsRequired;
    private Integer stock;
    private Integer status;
    private Integer sort;
}
