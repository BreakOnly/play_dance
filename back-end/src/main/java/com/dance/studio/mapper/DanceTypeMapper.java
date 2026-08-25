package com.dance.studio.mapper;

import com.dance.studio.entity.DanceType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DanceTypeMapper {
    List<DanceType> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);
    DanceType findById(@Param("id") Long id);
    int insert(DanceType d);
    int update(DanceType d);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
