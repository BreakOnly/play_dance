package com.dance.studio.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateReq {
    private String name;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private String avatar;
}
