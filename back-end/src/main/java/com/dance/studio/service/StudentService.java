package com.dance.studio.service;

import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.dto.StudentSaveReq;
import com.dance.studio.entity.Student;
import com.dance.studio.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台学员管理
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;

    private Long store() { return ContextHolder.storeId(); }

    public List<Student> list(String keyword, Integer status) {
        return studentMapper.list(store(), keyword, status);
    }

    public Student detail(Long id) {
        return studentMapper.findById(id);
    }

    public Student create(StudentSaveReq req) {
        Student s = new Student();
        s.setStoreId(store());
        s.setName(req.getName());
        s.setPhone(req.getPhone());
        s.setGender(req.getGender());
        s.setBirthday(req.getBirthday());
        s.setAvatar(req.getAvatar());
        s.setStage(req.getStage());
        s.setPreferDance(req.getPreferDance());
        s.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        studentMapper.insert(s);
        return studentMapper.findById(s.getId());
    }

    public Student update(Long id, StudentSaveReq req) {
        Student s = new Student();
        s.setId(id);
        s.setStoreId(store());
        s.setName(req.getName());
        s.setPhone(req.getPhone());
        s.setGender(req.getGender());
        s.setBirthday(req.getBirthday());
        s.setAvatar(req.getAvatar());
        s.setStatus(req.getStatus());
        studentMapper.adminUpdate(s);
        return studentMapper.findById(id);
    }
}
