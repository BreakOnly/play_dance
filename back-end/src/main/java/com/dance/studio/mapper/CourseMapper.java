package com.dance.studio.mapper;

import com.dance.studio.entity.Course;
import com.dance.studio.vo.CourseVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper {
    List<CourseVo> list(@Param("storeId") Long storeId,
                        @Param("type") String type,
                        @Param("danceTypeId") Long danceTypeId,
                        @Param("status") Integer status);
    CourseVo findVoById(@Param("id") Long id);
    Course findById(@Param("id") Long id);
    int insert(Course c);
    int update(Course c);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
