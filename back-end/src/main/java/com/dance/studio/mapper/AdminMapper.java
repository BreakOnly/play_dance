package com.dance.studio.mapper;

import com.dance.studio.entity.Admin;
import org.apache.ibatis.annotations.Param;

public interface AdminMapper {

    Admin findByUsername(@Param("username") String username);

    Admin findById(@Param("id") Long id);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateLastLogin(@Param("id") Long id);
}
