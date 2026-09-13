package com.dance.studio.mapper;

import com.dance.studio.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    List<Notice> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);

    Notice findById(@Param("id") Long id);

    void insert(Notice n);

    void update(Notice n);

    void deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
