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

    public void push(Long studentId, String type, String title, String content, String refType, Long refId) {
        Notification n = new Notification();
        n.setStudentId(studentId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRefType(refType);
        n.setRefId(refId);
        mapper.insert(n);
    }

    public List<Notification> list(Long studentId) { return mapper.listByStudent(studentId); }
    public int unread(Long studentId) { return mapper.countUnread(studentId); }
    public void read(Long studentId, Long id) { mapper.markRead(studentId, id); }
    public void readAll(Long studentId) { mapper.markAllRead(studentId); }
}
