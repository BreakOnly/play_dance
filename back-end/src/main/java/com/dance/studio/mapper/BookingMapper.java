package com.dance.studio.mapper;

import com.dance.studio.entity.Booking;
import com.dance.studio.vo.BookingVo;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingMapper {

    @org.apache.ibatis.annotations.Select(
            "SELECT COALESCE(MAX(waitlist_no),0)+1 FROM booking WHERE schedule_id=#{scheduleId}")
    int nextWaitlistNumber(Long scheduleId);


    @org.apache.ibatis.annotations.Select(
            "SELECT b.* FROM booking b JOIN course_schedule s ON s.id=b.schedule_id WHERE"
                    + " b.status='waitlist' AND s.start_time<=NOW() ORDER BY b.id")
    List<Booking> expiredWaitlists();

    int insert(Booking b);

    Booking findById(@Param("id") Long id);

    Booking findActiveByUserSchedule(
            @Param("userId") Long userId, @Param("scheduleId") Long scheduleId);

    /** 条件更新状态：仅当前状态为 expectStatus 时生效，返回影响行数（防并发重复流转） */
    int updateStatusIf(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("expectStatus") String expectStatus);

    /** 条件取消：仅当前状态为 expectStatus 时生效，返回影响行数（退款/扣减必须以此为凭） */
    int cancel(
            @Param("id") Long id,
            @Param("expectStatus") String expectStatus,
            @Param("cancelDeduct") int cancelDeduct);

    /** 某课次下所有"待上课/候补"的预约（课次取消时批量退还用） */
    List<Booking> listActiveBySchedule(@Param("scheduleId") Long scheduleId);

    /** 候补队列下一个（waitlist_no 最小） */
    Booking nextWaitlist(@Param("scheduleId") Long scheduleId);

    int countWaitlist(@Param("scheduleId") Long scheduleId);

    /** 设为待上课（候补转正） */
    int promote(@Param("id") Long id);

    /** 我的约课（按状态可选过滤） */
    List<BookingVo> listByUser(@Param("userId") Long userId, @Param("status") String status);

    /** 后台：按课次查约课（含候补） */
    List<BookingVo> listBySchedule(@Param("scheduleId") Long scheduleId);

    /** 某课次"待上课"且开课已过、未签到的预约（爽约判定用） */
    List<Booking> listNoShowCandidates(@Param("now") LocalDateTime now);

    /** 找该学员在某时刻可签到的待上课预约（按门店 + 时间窗，由 service 计算窗口） */
    List<Booking> listCheckinable(
            @Param("userId") Long userId,
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
