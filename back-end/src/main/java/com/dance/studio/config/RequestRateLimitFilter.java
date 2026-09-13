package com.dance.studio.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** 登录/上传的进程内限流；部署文档另提供反向代理限流。 */
@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private record Window(long minute, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getRequestURI();
        boolean login = path.endsWith("/auth/login"), upload = path.contains("/upload");
        if (!login && !upload) {
            chain.doFilter(req, res);
            return;
        }
        long minute = System.currentTimeMillis() / 60000;
        if (windows.size() > 10000)
            windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        String key = req.getRemoteAddr() + ":" + (login ? "login" : "upload");
        Window w =
                windows.compute(
                        key,
                        (k, v) ->
                                v == null || v.minute() != minute
                                        ? new Window(minute, 1)
                                        : new Window(minute, v.count() + 1));
        if (w.count() > (login ? 60 : 20)) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":1429,\"message\":\"操作过于频繁，请稍后再试\",\"data\":null}");
            return;
        }
        chain.doFilter(req, res);
    }
}
