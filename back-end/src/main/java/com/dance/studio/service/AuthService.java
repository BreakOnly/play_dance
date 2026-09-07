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
import com.dance.studio.entity.User;
import com.dance.studio.mapper.AdminMapper;
import com.dance.studio.mapper.UserMapper;
import com.dance.studio.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
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
        WechatService.WxSession session = wechatService.code2session(req.getCode());
        User user = userMapper.findByOpenid(session.openid());
        if (user == null) {
            // 首次登录自动建档（openid/unionid 落库）
            user = new User();
            user.setStoreId(DEFAULT_STORE_ID);
            user.setOpenid(session.openid());
            user.setUnionid(session.unionid());
            user.setStatus(1);
            userMapper.insert(user);
        } else if (session.unionid() != null && !session.unionid().equals(user.getUnionid())) {
            // 老用户回填 unionid（后绑定开放平台/历史数据）
            user.setUnionid(session.unionid());
            User patch = new User();
            patch.setId(user.getId());
            patch.setUnionid(session.unionid());
            userMapper.updateProfile(patch);
        }
        String token = jwtUtil.generate(new LoginUser(user.getId(), user.getStoreId(), LoginUser.TYPE_USER));
        return new LoginVo(token, user.getId(), user.getName());
    }

    /** 微信手机号授权：code 换手机号并绑定当前用户 */
    public String bindPhone(String code) {
        String phone = wechatService.phoneNumber(code);
        User patch = new User();
        patch.setId(ContextHolder.userId());
        patch.setPhone(phone);
        userMapper.updateProfile(patch);
        return phone;
    }

    public User mpProfile() {
        return userMapper.findById(ContextHolder.userId());
    }

    public User updateProfile(ProfileUpdateReq req) {
        User s = new User();
        s.setId(ContextHolder.userId());
        s.setName(req.getName());
        s.setPhone(req.getPhone());
        s.setGender(req.getGender());
        s.setBirthday(req.getBirthday());
        s.setAvatar(req.getAvatar());
        userMapper.updateProfile(s);
        return userMapper.findById(s.getId());
    }
}
