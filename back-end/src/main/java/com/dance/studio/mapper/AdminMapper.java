package com.dance.studio.mapper;

import com.dance.studio.entity.Admin;

import org.apache.ibatis.annotations.Param;

public interface AdminMapper {
    @org.apache.ibatis.annotations.Update(
            "UPDATE admin SET token_version=token_version+1 WHERE id=#{id}")
    int revoke(Long id);

    Admin findByUsername(@Param("username") String username);

    Admin findById(@Param("id") Long id);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateLastLogin(@Param("id") Long id);
}
