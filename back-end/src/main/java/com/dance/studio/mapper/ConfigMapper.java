package com.dance.studio.mapper;

import com.dance.studio.entity.Config;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConfigMapper {

    List<Config> listByStore(@Param("storeId") Long storeId);

    Config findByKey(@Param("storeId") Long storeId, @Param("key") String key);

    int update(@Param("storeId") Long storeId, @Param("key") String key, @Param("value") String value);
}
