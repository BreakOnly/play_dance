package com.dance.studio.mapper;

import com.dance.studio.entity.CourseSchedule;
import com.dance.studio.vo.ScheduleVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CourseScheduleMapper {

    List<ScheduleVo> list(@Param("storeId") Long storeId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("courseId") Long courseId,
                          @Param("teacherId") Long teacherId,
                          @Param("userId") Long userId);

    /** 小程序课表：跨门店（启用门店），支持关键字(课程/老师/舞种/曲目)与舞种/难度/门店筛选 */
    List<ScheduleVo> listForMp(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               @Param("keyword") String keyword,
                               @Param("danceTypeId") Long danceTypeId,
                               @Param("levelId") Long levelId,
                               @Param("storeId") Long storeId,
                               @Param("userId") Long userId);

    ScheduleVo findVoById(@Param("id") Long id, @Param("userId") Long userId);

    CourseSchedule findById(@Param("id") Long id);

    int insert(CourseSchedule s);

    int update(CourseSchedule s);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 冲突检测：同教师或同教室时间重叠（排除自身、排除已取消） */
    int countConflict(@Param("storeId") Long storeId,
                      @Param("teacherId") Long teacherId,
                      @Param("room") String room,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end,
                      @Param("excludeId") Long excludeId);

    /** 约课占位：booked_count+1 且未满；返回受影响行数 */
    int incrBooked(@Param("id") Long id);

    /** 释放占位：booked_count-1（不为负） */
    int decrBooked(@Param("id") Long id);

    /** 结束已过期的课次 */
    int finishExpired(@Param("now") LocalDateTime now);

    /** 统计引用某注意事项的排课数（删除注意事项前校验用） */
    int countByNotice(@Param("noticeId") Long noticeId, @Param("storeId") Long storeId);
}
