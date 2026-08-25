package com.dance.studio.mapper;

import com.dance.studio.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper {
    int insert(Notification n);
    List<Notification> listByStudent(@Param("studentId") Long studentId);
    int countUnread(@Param("studentId") Long studentId);
    int markRead(@Param("studentId") Long studentId, @Param("id") Long id);
    int markAllRead(@Param("studentId") Long studentId);
}
