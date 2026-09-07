package com.dance.studio.common.util;

import com.dance.studio.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：双端共用，token 内含 userId / storeId / type
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.admin-expire-hours:12}")
    private long adminExpireHours;
    @Value("${app.jwt.user-expire-hours:168}")
    private long userExpireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(LoginUser user) {
        long hours = LoginUser.TYPE_ADMIN.equals(user.getType()) ? adminExpireHours : userExpireHours;
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("storeId", user.getStoreId())
                .claim("type", user.getType())
                .issuedAt(new Date(now))
                .expiration(new Date(now + hours * 3600_000L))
                .signWith(key())
                .compact();
    }

    /** 解析失败返回 null */
    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(c.getSubject());
            Long storeId = c.get("storeId", Number.class).longValue();
            String type = c.get("type", String.class);
            return new LoginUser(userId, storeId, type);
        } catch (Exception e) {
            return null;
        }
    }
}
