package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.context.LoginUser;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.common.util.JwtUtil;
import com.dance.studio.dto.AdminLoginReq;
import com.dance.studio.dto.MpLoginReq;
import com.dance.studio.dto.ProfileUpdateReq;
import com.dance.studio.entity.Admin;
import com.dance.studio.entity.Student;
import com.dance.studio.mapper.AdminMapper;
import com.dance.studio.mapper.StudentMapper;
import com.dance.studio.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminMapper adminMapper;
    private final StudentMapper studentMapper;
    private final WechatService wechatService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 默认门店（多门店扩展时按域名/参数路由） */
    private static final Long DEFAULT_STORE_ID = 1L;

    // ---------- 后台登录 ----------
    public LoginVo adminLogin(AdminLoginReq req) {
        Admin admin = adminMapper.findByUsername(req.getUsername());
        if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ResultCode.LOGIN_FAIL);
        }
        if (!passwordEncoder.matches(req.getPassword(), admin.getPasswordHash())) {
            throw new BizException(ResultCode.LOGIN_FAIL);
        }
        adminMapper.updateLastLogin(admin.getId());
        String token = jwtUtil.generate(new LoginUser(admin.getId(), admin.getStoreId(), LoginUser.TYPE_ADMIN));
        return new LoginVo(token, admin.getId(), admin.getNickname());
    }

    public Admin adminInfo() {
        return adminMapper.findById(ContextHolder.userId());
    }

    // ---------- 小程序登录 ----------
    public LoginVo mpLogin(MpLoginReq req) {
        String openid = wechatService.code2openid(req.getCode());
        Student student = studentMapper.findByOpenid(openid);
        if (student == null) {
            // 首次登录自动建档
            student = new Student();
            student.setStoreId(DEFAULT_STORE_ID);
            student.setOpenid(openid);
            student.setStatus(1);
            studentMapper.insert(student);
        }
        String token = jwtUtil.generate(new LoginUser(student.getId(), student.getStoreId(), LoginUser.TYPE_STUDENT));
        return new LoginVo(token, student.getId(), student.getName());
    }

    public Student mpProfile() {
        return studentMapper.findById(ContextHolder.userId());
    }

    public Student updateProfile(ProfileUpdateReq req) {
        Student s = new Student();
        s.setId(ContextHolder.userId());
        s.setName(req.getName());
        s.setPhone(req.getPhone());
        s.setGender(req.getGender());
        s.setBirthday(req.getBirthday());
        s.setAvatar(req.getAvatar());
        studentMapper.updateProfile(s);
        return studentMapper.findById(s.getId());
    }
}
