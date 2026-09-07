package com.dance.studio.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserSaveReq {
    private String name;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private String avatar;
    private Integer status;
}
