package com.dance.studio.mapper;

import com.dance.studio.entity.Checkin;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinMapper {

    int insert(Checkin c);

    Checkin findByBooking(@Param("bookingId") Long bookingId);

    /** 后台签到记录（按门店/日期范围/课次） */
    List<java.util.Map<String, Object>> listRecords(@Param("storeId") Long storeId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to,
                                                    @Param("scheduleId") Long scheduleId);

    /** 出勤统计：某门店时间段内 签到数/约课数/爽约数/消耗课时 */
    java.util.Map<String, Object> attendanceStat(@Param("storeId") Long storeId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
}
