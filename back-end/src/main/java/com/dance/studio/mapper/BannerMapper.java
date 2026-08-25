package com.dance.studio.mapper;

import com.dance.studio.entity.Banner;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BannerMapper {
    List<Banner> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);
    Banner findById(@Param("id") Long id);
    int insert(Banner b);
    int update(Banner b);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
