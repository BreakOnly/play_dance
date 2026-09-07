package com.dance.studio.service;

import com.dance.studio.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 微信小程序服务：code 换会话（openid/unionid）、手机号授权、access_token 缓存。
 * 开发期 app.wechat.mock=true 时：code 直接当 openid；手机号接口的 code 直接当手机号（便于自测联调）。
 */
@Slf4j
@Service
public class WechatService {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={token}";

    @Value("${app.wechat.mock:true}")
    private boolean mock;
    @Value("${app.wechat.appid:}")
    private String appid;
    @Value("${app.wechat.secret:}")
    private String secret;

    private final RestClient restClient = RestClient.create();

    /** code2session 结果：openid 必有；unionid 需小程序绑定微信开放平台后才返回 */
    public record WxSession(String openid, String unionid) {}

    /** 登录 code 换 openid/unionid */
    public WxSession code2session(String code) {
        if (mock) {
            // 开发：把 code 当作稳定 openid，方便用同一 code 复现同一用户
            return new WxSession("mock_" + code, null);
        }
        requireAppidConfig();
        Map<String, Object> resp = getForMap(JSCODE2SESSION_URL, appid, secret, code);
        if (resp == null || resp.get("openid") == null) {
            log.warn("jscode2session 失败: {}", resp);
            throw new BizException("微信登录失败");
        }
        return new WxSession((String) resp.get("openid"), (String) resp.get("unionid"));
    }

    /**
     * 手机号授权 code 换手机号（需企业主体小程序且已开通手机号验证组件）。
     * mock 模式下 code 即手机号，供开发联调。
     */
    @SuppressWarnings("unchecked")
    public String phoneNumber(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("授权code为空");
        }
        if (mock) {
            return code.trim();
        }
        requireAppidConfig();
        Map<String, Object> resp = restClient.post()
                .uri(PHONE_URL, accessToken())
                .body(Map.of("code", code))
                .retrieve()
                .body(Map.class);
        if (resp == null || !Integer.valueOf(0).equals(toInt(resp.get("errcode")))) {
            log.warn("getuserphonenumber 失败: {}", resp);
            throw new BizException("手机号授权失败");
        }
        Map<String, Object> phoneInfo = (Map<String, Object>) resp.get("phone_info");
        if (phoneInfo == null) {
            throw new BizException("手机号授权失败");
        }
        String phone = (String) phoneInfo.getOrDefault("purePhoneNumber", phoneInfo.get("phoneNumber"));
        if (phone == null || phone.isBlank()) {
            throw new BizException("手机号授权失败");
        }
        return phone;
    }

    /** access_token 内存缓存：有效期 7200s，提前 5 分钟过期 */
    private volatile String cachedToken;
    private volatile long tokenExpireAt = 0;

    private synchronized String accessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpireAt) {
            return cachedToken;
        }
        Map<String, Object> resp = getForMap(TOKEN_URL, appid, secret);
        if (resp == null || resp.get("access_token") == null) {
            log.warn("获取access_token失败: {}", resp);
            throw new BizException("获取access_token失败");
        }
        cachedToken = (String) resp.get("access_token");
        long expiresIn = resp.get("expires_in") != null ? toInt(resp.get("expires_in")) : 7200;
        tokenExpireAt = now + Math.max(expiresIn - 300, 60) * 1000L;
        return cachedToken;
    }

    private void requireAppidConfig() {
        if (appid.isBlank() || secret.isBlank()) {
            throw new BizException("未配置微信 appid/secret");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getForMap(String url, Object... uriVars) {
        return restClient.get().uri(url, uriVars).retrieve().body(Map.class);
    }

    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : -1;
    }
}
