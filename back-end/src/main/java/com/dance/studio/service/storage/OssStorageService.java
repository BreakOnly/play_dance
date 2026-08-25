package com.dance.studio.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.dance.studio.common.exception.BizException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云 OSS 存储实现。上线设置 app.storage.type=oss 时启用。
 * - 图片等公共资源：返回直链（建议 bucket 公共读或绑定 CDN）
 * - 视频等私有资源：signedUrl() 返回带时效的签名 URL（建议 bucket 私有读写）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "oss")
public class OssStorageService implements StorageService {

    @Value("${app.storage.oss.endpoint}")
    private String endpoint;
    @Value("${app.storage.oss.bucket}")
    private String bucket;
    @Value("${app.storage.oss.access-key-id}")
    private String ak;
    @Value("${app.storage.oss.access-key-secret}")
    private String sk;
    @Value("${app.storage.oss.url-expire-seconds:3600}")
    private long urlExpireSeconds;

    private OSS client;

    @PostConstruct
    public void init() {
        this.client = new OSSClientBuilder().build(endpoint, ak, sk);
        log.info("OSS 存储已启用 bucket={} endpoint={}", bucket, endpoint);
    }

    @PreDestroy
    public void destroy() {
        if (client != null) client.shutdown();
    }

    @Override
    public String upload(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) throw new BizException("文件为空");
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
        String key = dir + "/" + LocalDate.now() + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            if (file.getContentType() != null) meta.setContentType(file.getContentType());
            client.putObject(bucket, key, file.getInputStream(), meta);
        } catch (IOException e) {
            log.error("OSS 上传失败", e);
            throw new BizException("文件上传失败");
        }
        // 返回直链；私有资源播放时再走 signedUrl()
        return "https://" + bucket + "." + endpoint + "/" + key;
    }

    @Override
    public String signedUrl(String url, long expireSeconds) {
        if (url == null) return null;
        String key = extractKey(url);
        if (key == null) return url;
        long exp = expireSeconds > 0 ? expireSeconds : urlExpireSeconds;
        Date expiration = new Date(System.currentTimeMillis() + exp * 1000);
        return client.generatePresignedUrl(bucket, key, expiration).toString();
    }

    /** 从直链中解析出对象 key */
    private String extractKey(String url) {
        String host = bucket + "." + endpoint + "/";
        int idx = url.indexOf(host);
        if (idx >= 0) return url.substring(idx + host.length());
        // 已是 key 形式
        return url.startsWith("http") ? null : url;
    }
}
