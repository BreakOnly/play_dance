package com.dance.studio.interceptor;

import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.context.LoginUser;
import com.dance.studio.common.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.function.Predicate;

/**
 * 鉴权拦截器：解析 Bearer token，校验类型，写入上下文。 同一个类实例化两个 Bean（admin/user），构造时指定所需类型。 userExists 可空：提供时额外校验
 * token 主体（如学员记录）仍存在且有效， 避免开发期重置数据库后旧 token 指向已删除记录，业务层解引用出 NPE 变成"系统异常"。
 */
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final String requiredType;
    private final Predicate<LoginUser> userExists;

    public AuthInterceptor(JwtUtil jwtUtil, String requiredType) {
        this(jwtUtil, requiredType, null);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 放行预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            LoginUser user = jwtUtil.parse(token);
            if (user != null && requiredType.equals(user.getType())) {
                if (userExists != null && !userExists.test(user)) {
                    // token 未过期但主体已删除/停用：按 1002 返回，前端清 token 重新登录
                    writeUnauthorized(response, "账号不存在或已停用，请重新登录");
                    return false;
                }
                ContextHolder.set(user);
                return true;
            }
        }
        writeUnauthorized(response, "未登录或登录已过期");
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":1002,\"message\":\"" + message + "\",\"data\":null}");
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        ContextHolder.clear();
    }
}
