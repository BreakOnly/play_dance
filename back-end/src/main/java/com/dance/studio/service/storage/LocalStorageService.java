package com.dance.studio.service.storage;

import com.dance.studio.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地存储实现（开发期，默认启用）。
 * 上线设置 app.storage.type=oss 即切换到 {@link OssStorageService}。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local-dir:./uploads}")
    private String localDir;
    @Value("${app.storage.base-url:http://localhost:8080/files}")
    private String baseUrl;

    @Override
    public String upload(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件为空");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
        String datePath = LocalDate.now().toString();
        String relative = dir + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(localDir, relative);
        if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
            throw new BizException("创建目录失败");
        }
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException("文件保存失败");
        }
        return baseUrl + "/" + relative;
    }

    @Override
    public String signedUrl(String url, long expireSeconds) {
        // 本地开发不做签名，直接返回
        return url;
    }
}
