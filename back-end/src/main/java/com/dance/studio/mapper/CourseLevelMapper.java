package com.dance.studio.mapper;

import com.dance.studio.entity.CourseLevel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseLevelMapper {
    List<CourseLevel> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);
    /** 统计同门店下已占用某星级的记录数（excludeId 用于编辑时排除自身） */
    int countBySort(@Param("storeId") Long storeId, @Param("sort") Integer sort, @Param("excludeId") Long excludeId);
    CourseLevel findById(@Param("id") Long id);
    int insert(CourseLevel d);
    int update(CourseLevel d);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
