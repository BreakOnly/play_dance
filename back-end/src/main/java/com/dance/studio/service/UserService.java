package com.dance.studio.service;

import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.dto.UserSaveReq;
import com.dance.studio.entity.User;
import com.dance.studio.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/** 后台学员管理 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    private Long store() {
        return ContextHolder.storeId();
    }

    public List<User> list(String keyword, Integer status) {
        return userMapper.list(store(), keyword, status);
    }

    public User detail(Long id) {
        return userMapper.findById(id);
    }

    public User update(Long id, UserSaveReq req) {
        User s = new User();
        s.setId(id);
        s.setStoreId(store());
        s.setName(req.getName());
        s.setGender(req.getGender());
        s.setBirthday(req.getBirthday());
        s.setAvatar(req.getAvatar());
        userMapper.adminUpdate(s);
        return userMapper.findById(id);
    }
}
