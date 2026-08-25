package com.dance.studio.mapper;

import com.dance.studio.entity.Teacher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TeacherMapper {
    List<Teacher> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);
    Teacher findById(@Param("id") Long id);
    int insert(Teacher t);
    int update(Teacher t);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
