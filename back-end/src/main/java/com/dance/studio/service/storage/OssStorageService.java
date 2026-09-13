package com.dance.studio.service.storage;

import com.aliyun.oss.ClientBuilderConfiguration;
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
    /** endpoint 为 IP/localhost（如本地 oss-emulator）时无法用虚拟主机风格，改 path-style 访问 */
    private boolean pathStyleEndpoint;

    @PostConstruct
    public void init() {
        String host = endpoint.replaceFirst("^https?://", "").split(":")[0];
        pathStyleEndpoint = host.equals("localhost") || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
        if (pathStyleEndpoint) {
            ClientBuilderConfiguration cfg = new ClientBuilderConfiguration();
            cfg.setSLDEnabled(true);
            this.client = new OSSClientBuilder().build(endpoint, ak, sk, cfg);
        } else {
            this.client = new OSSClientBuilder().build(endpoint, ak, sk);
        }
        log.info("OSS 存储已启用 bucket={} endpoint={} pathStyle={}", bucket, endpoint, pathStyleEndpoint);
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
        // 返回直链；私有资源播放时再走 signedUrl()。
        // 云端 endpoint 用虚拟主机风格 https://{bucket}.{endpoint}/；
        // 本地模拟器(IP)用 path-style http://{endpoint}/{bucket}/
        String hostOnly = endpoint.replaceFirst("^https?://", "");
        if (pathStyleEndpoint) {
            return (endpoint.startsWith("https://") ? "https://" : "http://") + hostOnly + "/" + bucket + "/" + key;
        }
        return (endpoint.startsWith("http://") ? "http://" : "https://") + bucket + "." + hostOnly + "/" + key;
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

    /** 从直链中解析出对象 key（兼容虚拟主机风格与 path-style，忽略可能携带的查询串） */
    private String extractKey(String url) {
        if (!url.startsWith("http")) return url; // 已是 key 形式
        String path = url.split("\\?", 2)[0];
        String hostOnly = endpoint.replaceFirst("^https?://", "");
        String vhPrefix = bucket + "." + hostOnly + "/";
        int idx = path.indexOf(vhPrefix);
        if (idx >= 0) return path.substring(idx + vhPrefix.length());
        String psPrefix = "/" + bucket + "/";
        idx = path.indexOf(psPrefix);
        if (idx >= 0) return path.substring(idx + psPrefix.length());
        return null;
    }
}
