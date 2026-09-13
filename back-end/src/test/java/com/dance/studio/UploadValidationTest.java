package com.dance.studio;

import static org.junit.jupiter.api.Assertions.*;

import com.dance.studio.common.exception.BizException;
import com.dance.studio.service.storage.UploadValidation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

class UploadValidationTest {
    @Test
    void rejectsDisguisedHtmlAndSvg() {
        assertThrows(
                BizException.class,
                () ->
                        UploadValidation.validate(
                                new MockMultipartFile(
                                        "file",
                                        "x.png",
                                        "image/png",
                                        "<html><script>1</script></html>".getBytes()),
                                "image"));
        assertThrows(
                BizException.class,
                () ->
                        UploadValidation.validate(
                                new MockMultipartFile(
                                        "file", "x.svg", "image/svg+xml", "<svg></svg>".getBytes()),
                                "image"));
    }

    @Test
    void normalizesImageAndRemovesUntrustedFilename() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "jpg", out);
        var file =
                UploadValidation.validate(
                        new MockMultipartFile("file", "evil.html", "text/html", out.toByteArray()),
                        "image");
        assertEquals("image.png", file.getOriginalFilename());
        assertEquals("image/png", file.getContentType());
        assertNotNull(ImageIO.read(file.getInputStream()));
    }

    @Test
    void rejectsFakeMp4() {
        assertThrows(
                BizException.class,
                () ->
                        UploadValidation.validate(
                                new MockMultipartFile("file", "x.mp4", "video/mp4", new byte[30]),
                                "video"));
    }
}
