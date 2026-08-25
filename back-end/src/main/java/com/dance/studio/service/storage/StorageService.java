package com.dance.studio.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * 上传文件，返回可访问 URL
     * @param file 文件
     * @param dir  子目录（如 image / video）
     */
    String upload(MultipartFile file, String dir);

    /**
     * 生成带时效的访问地址（本地实现直接返回原地址；OSS 实现返回签名 URL）
     */
    String signedUrl(String url, long expireSeconds);
}
