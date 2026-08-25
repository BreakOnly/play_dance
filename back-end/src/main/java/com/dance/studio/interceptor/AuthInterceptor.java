package com.dance.studio.interceptor;

import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.context.LoginUser;
import com.dance.studio.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：解析 Bearer token，校验类型，写入上下文。
 * 同一个类实例化两个 Bean（admin/student），构造时指定所需类型。
 */
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final String requiredType;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            LoginUser user = jwtUtil.parse(token);
            if (user != null && requiredType.equals(user.getType())) {
                ContextHolder.set(user);
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":1002,\"message\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ContextHolder.clear();
    }
}
