package com.dance.studio.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DashboardMapper {

    Map<String, Object> overview(@Param("storeId") Long storeId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /** 课时不足学员 */
    List<Map<String, Object>> lowHoursStudents(@Param("storeId") Long storeId, @Param("threshold") int threshold);

    /** N 天未到课学员（可能流失） */
    List<Map<String, Object>> inactiveStudents(@Param("storeId") Long storeId, @Param("before") LocalDateTime before);
}
