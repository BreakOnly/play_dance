package com.dance.studio.mapper;

import com.dance.studio.entity.PointLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PointLogMapper {
    int insert(PointLog log);
    List<PointLog> listByUser(@Param("userId") Long userId);
}
