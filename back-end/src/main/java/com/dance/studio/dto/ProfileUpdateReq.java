package com.dance.studio.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateReq {
    @jakarta.validation.constraints.Size(max = 50)
    private String name;

    private String phone;

    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(2)
    private Integer gender;

    @jakarta.validation.constraints.PastOrPresent private LocalDate birthday;

    @jakarta.validation.constraints.Size(max = 255)
    private String avatar;
}
