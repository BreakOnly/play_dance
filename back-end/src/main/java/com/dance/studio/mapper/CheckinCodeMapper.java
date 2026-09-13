package com.dance.studio.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

public interface CheckinCodeMapper {
    @Insert(
            "INSERT INTO checkin_code(token_hash,schedule_id,expires_at)"
                    + " VALUES(#{hash},#{scheduleId},#{expires})")
    int insert(
            @Param("hash") String hash,
            @Param("scheduleId") Long scheduleId,
            @Param("expires") LocalDateTime expires);

    @Select("SELECT schedule_id FROM checkin_code WHERE token_hash=#{hash} AND expires_at>NOW()")
    Long resolve(String hash);

    @Delete("DELETE FROM checkin_code WHERE expires_at<DATE_SUB(NOW(),INTERVAL 1 DAY)")
    int cleanup();
}
