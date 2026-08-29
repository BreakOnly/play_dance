package com.dance.studio.mapper;

import com.dance.studio.entity.Room;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomMapper {
    List<Room> listByStore(@Param("storeId") Long storeId, @Param("onlyEnabled") boolean onlyEnabled);
    Room findById(@Param("id") Long id);
    int insert(Room d);
    int update(Room d);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);
}
