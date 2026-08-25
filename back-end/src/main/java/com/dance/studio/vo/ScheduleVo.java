package com.dance.studio.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排课/课次（含课程、教师等展示信息）
 */
@Data
public class ScheduleVo {
    private Long id;
    private Long storeId;
    /** 门店名（小程序按门店分组展示） */
    private String storeName;
    private Long courseId;
    private String courseName;
    private String courseType;
    private String danceTypeName;
    /** 舞种ID（前端筛选项聚合用） */
    private Long danceTypeId;
    /** 舞种介绍（详情页展示） */
    private String danceTypeIntro;
    private String levelName;
    /** 难度ID（前端筛选项聚合用） */
    private Long levelId;
    /** 星级(1-5)，取自难度等级的sort字段，小程序详情页展示 */
    private Integer levelSort;
    private Integer durationMin;
    private Integer costHours;
    /** 课程简介（course.description） */
    private String description;
    private Long teacherId;
    private String teacherName;
    /** 教师头像（teacher.avatar） */
    private String teacherAvatar;
    /** 教师简介（详情页展示） */
    private String teacherIntro;
    private String room;
    /** 本课次曲目 */
    private String song;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    private String status;
    /** 小程序场景：当前学员对该课次的约课状态（booked/checked/waitlist/null） */
    private String myBookingStatus;
}
