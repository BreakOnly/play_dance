package com.dance.studio.mapper;

import com.dance.studio.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper {
    int insert(Notification n);
    List<Notification> listByUser(@Param("userId") Long userId);
    int countUnread(@Param("userId") Long userId);
    int markRead(@Param("userId") Long userId, @Param("id") Long id);
    int markAllRead(@Param("userId") Long userId);
}
