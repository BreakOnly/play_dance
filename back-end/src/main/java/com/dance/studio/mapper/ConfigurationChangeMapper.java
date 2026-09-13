package com.dance.studio.mapper;

import org.apache.ibatis.annotations.*;

public interface ConfigurationChangeMapper {
    @Insert(
            "INSERT INTO configuration_change(admin_id,config_key,old_value,new_value)"
                    + " VALUES(#{admin},#{key},#{old},#{value})")
    int insert(
            @Param("admin") Long admin,
            @Param("key") String key,
            @Param("old") String old,
            @Param("value") String value);
}
