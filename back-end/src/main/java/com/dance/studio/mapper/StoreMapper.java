package com.dance.studio.mapper;

import com.dance.studio.entity.Store;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StoreMapper {

    Store findById(@Param("id") Long id);

    /** 启用中的门店（小程序课表分组/筛选用） */
    List<Store> listEnabled();
}
