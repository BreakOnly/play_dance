package com.dance.studio.mapper;

import com.dance.studio.entity.HourAllocation;

import org.apache.ibatis.annotations.*;

import java.util.List;

public interface HourAllocationMapper {
    @Insert(
            "INSERT INTO booking_hour_allocation(booking_id,batch_id,hours,state)"
                    + " VALUES(#{bookingId},#{batchId},#{hours},'locked') ON DUPLICATE KEY UPDATE"
                    + " hours=VALUES(hours),state='locked'")
    int insert(HourAllocation allocation);

    @Select(
            "SELECT a.*, b.expire_at FROM booking_hour_allocation a JOIN course_hour_batch b ON"
                + " b.id=a.batch_id WHERE a.booking_id=#{bookingId} AND a.state='locked' ORDER BY"
                + " a.batch_id FOR UPDATE")
    List<HourAllocation> locked(Long bookingId);

    @Update(
            "UPDATE booking_hour_allocation SET state=#{state} WHERE booking_id=#{bookingId} AND"
                    + " batch_id=#{batchId} AND state='locked'")
    int finish(
            @Param("bookingId") Long bookingId,
            @Param("batchId") Long batchId,
            @Param("state") String state);
}
