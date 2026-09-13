package com.dance.studio.common.util;

import com.dance.studio.common.context.LoginUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

/** JWT 工具：双端共用，token 内含 userId / storeId / type */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.admin-expire-hours:12}")
    private long adminExpireHours;

    @Value("${app.jwt.user-expire-hours:168}")
    private long userExpireHours;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    private SecretKey signingKey;

    @jakarta.annotation.PostConstruct
    public void initialize() {
        boolean local =
                environment.acceptsProfiles(
                        org.springframework.core.env.Profiles.of("local", "test"));
        if (secret.isBlank() && local) signingKey = Jwts.SIG.HS256.key().build();
        else {
            if (secret.getBytes(StandardCharsets.UTF_8).length < 64
                    || secret.toLowerCase().contains("dance-studio")
                    || secret.toLowerCase().contains("change"))
                throw new IllegalStateException("请配置至少 64 字节的独立随机 JWT_SECRET");
            signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    private SecretKey key() {
        return signingKey;
    }

    public String generate(LoginUser user) {
        long hours =
                LoginUser.TYPE_ADMIN.equals(user.getType()) ? adminExpireHours : userExpireHours;
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("storeId", user.getStoreId())
                .claim("type", user.getType())
                .claim("ver", user.getTokenVersion())
                .issuedAt(new Date(now))
                .expiration(new Date(now + hours * 3600_000L))
                .signWith(key())
                .compact();
    }

    /** 解析失败返回 null */
    public LoginUser parse(String token) {
        try {
            Claims c =
                    Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(c.getSubject());
            Long storeId = c.get("storeId", Number.class).longValue();
            String type = c.get("type", String.class);
            LoginUser user = new LoginUser(userId, storeId, type);
            Number version = c.get("ver", Number.class);
            if (version == null) return null;
            user.setTokenVersion(version.intValue());
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}
