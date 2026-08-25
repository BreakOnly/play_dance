package com.dance.studio.common.context;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;

/**
 * 基于 ThreadLocal 的登录上下文
 */
public class ContextHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 必须已登录，否则抛异常 */
    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return u;
    }

    public static Long userId() {
        return require().getUserId();
    }

    public static Long storeId() {
        LoginUser u = HOLDER.get();
        return u != null ? u.getStoreId() : 1L;
    }
}
