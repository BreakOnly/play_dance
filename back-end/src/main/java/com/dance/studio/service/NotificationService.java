package com.dance.studio.service;

import com.dance.studio.entity.Notification;
import com.dance.studio.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper mapper;

    public void push(Long userId, String type, String title, String content, String refType, Long refId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRefType(refType);
        n.setRefId(refId);
        mapper.insert(n);
    }

    public List<Notification> list(Long userId) { return mapper.listByUser(userId); }
    public int unread(Long userId) { return mapper.countUnread(userId); }
    public void read(Long userId, Long id) { mapper.markRead(userId, id); }
    public void readAll(Long userId) { mapper.markAllRead(userId); }
}
