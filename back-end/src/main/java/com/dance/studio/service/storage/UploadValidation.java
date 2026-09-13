package com.dance.studio.service.storage;

import com.dance.studio.common.exception.BizException;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;

import javax.imageio.ImageIO;

public final class UploadValidation {
    private UploadValidation() {}

    public static MultipartFile validate(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) throw new BizException("文件不能为空");
        try {
            if ("image".equals(kind)) {
                if (file.getSize() > 10 * 1024 * 1024) throw new BizException("图片不能超过 10MB");
                try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
                    var readers = ImageIO.getImageReaders(stream);
                    if (!readers.hasNext()) throw new BizException("请选择有效的 PNG 或 JPEG 图片");
                    var reader = readers.next();
                    try {
                        reader.setInput(stream);
                        String format = reader.getFormatName().toLowerCase();
                        if (!format.equals("png")
                                && !format.equals("jpeg")
                                && !format.equals("jpg"))
                            throw new BizException("仅支持 PNG 和 JPEG 图片");
                        if ((long) reader.getWidth(0) * reader.getHeight(0) > 16000000)
                            throw new BizException("图片尺寸过大");
                        var image = reader.read(0);
                        var out = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", out);
                        if (out.size() > 20 * 1024 * 1024) throw new BizException("处理后图片过大，请压缩后上传");
                        return new ImageFile(out.toByteArray());
                    } finally {
                        reader.dispose();
                    }
                }
            }
            if (!"video".equals(kind) || file.getSize() > 200L * 1024 * 1024)
                throw new BizException("视频不能超过 200MB");
            try (var stream = file.getInputStream()) {
                byte[] header = stream.readNBytes(12);
                if (header.length < 12
                        || !new String(header, 4, 4, java.nio.charset.StandardCharsets.US_ASCII)
                                .equals("ftyp")
                        || file.getOriginalFilename() == null
                        || !file.getOriginalFilename().toLowerCase().endsWith(".mp4"))
                    throw new BizException("仅支持 MP4 视频");
            }
            return file;
        } catch (IOException e) {
            throw new BizException("文件无法读取");
        }
    }

    private record ImageFile(byte[] bytes) implements MultipartFile {
        public String getName() {
            return "file";
        }

        public String getOriginalFilename() {
            return "image.png";
        }

        public String getContentType() {
            return "image/png";
        }

        public boolean isEmpty() {
            return bytes.length == 0;
        }

        public long getSize() {
            return bytes.length;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
