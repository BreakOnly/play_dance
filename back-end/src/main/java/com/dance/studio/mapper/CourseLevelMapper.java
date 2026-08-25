package com.dance.studio.mapper;

import com.dance.studio.entity.CourseLevel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseLevelMapper {
    List<CourseLevel> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);
    CourseLevel findById(@Param("id") Long id);
    int insert(CourseLevel d);
    int update(CourseLevel d);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
