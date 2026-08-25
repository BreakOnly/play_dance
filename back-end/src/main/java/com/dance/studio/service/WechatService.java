package com.dance.studio.service;

import com.dance.studio.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 微信小程序登录：code 换 openid。
 * 开发期 app.wechat.mock=true 时，直接用 code 当 openid，免真实 appid/secret。
 */
@Slf4j
@Service
public class WechatService {

    @Value("${app.wechat.mock:true}")
    private boolean mock;
    @Value("${app.wechat.appid:}")
    private String appid;
    @Value("${app.wechat.secret:}")
    private String secret;

    private final RestClient restClient = RestClient.create();

    /**
     * @return openid
     */
    public String code2openid(String code) {
        if (mock) {
            // 开发：把 code 当作稳定 openid，方便用同一 code 复现同一学员
            return "mock_" + code;
        }
        if (appid.isBlank() || secret.isBlank()) {
            throw new BizException("未配置微信 appid/secret");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restClient.get()
                .uri(url, appid, secret, code)
                .retrieve()
                .body(Map.class);
        if (resp == null || resp.get("openid") == null) {
            log.warn("jscode2session 失败: {}", resp);
            throw new BizException("微信登录失败");
        }
        return (String) resp.get("openid");
    }
}
