package com.dance.studio.mapper;

import com.dance.studio.entity.HourLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HourLogMapper {
    int insert(HourLog log);
    List<HourLog> listByStudent(@Param("studentId") Long studentId);
}
