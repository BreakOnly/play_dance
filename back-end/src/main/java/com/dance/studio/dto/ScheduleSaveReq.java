package com.dance.studio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleSaveReq {
    @NotNull(message = "请选择课程")
    private Long courseId;
    private Long teacherId;
    private String room;
    /** 本课次曲目（可选） */
    private String song;
    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;
    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;
    /** 不传则用课程默认容量 */
    private Integer capacity;
}
