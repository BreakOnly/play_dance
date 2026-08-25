package com.dance.studio.mapper;

import com.dance.studio.entity.Student;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentMapper {

    Student findById(@Param("id") Long id);

    Student findByOpenid(@Param("openid") String openid);

    int insert(Student student);

    int updateProfile(Student student);

    // ---- 后台管理 ----
    List<Student> list(@Param("storeId") Long storeId,
                       @Param("keyword") String keyword,
                       @Param("status") Integer status);

    int adminUpdate(Student student);

    // ---- 余额变更（带条件保证不为负）----
    int addCourseBalance(@Param("id") Long id, @Param("delta") int delta);

    int addFrozen(@Param("id") Long id, @Param("delta") int delta);

    /** 返回受影响行数；delta 为负时若余额不足则不更新(返回0) */
    int addPoints(@Param("id") Long id, @Param("delta") int delta);
}
