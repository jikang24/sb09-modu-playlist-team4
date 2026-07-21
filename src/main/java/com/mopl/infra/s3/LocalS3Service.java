package com.mopl.infra.s3;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile({"local","dev"})
public class LocalS3Service implements S3Service {
    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String ext = extractExtension(file.getOriginalFilename());
        validateImageContent(file);

        try {
            Files.createDirectories(UPLOAD_DIR);
            String fileName = UUID.randomUUID() + "." + ext;
            Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("[로컬] 파일 저장: {}", fileName);
            return "/uploads/" + fileName;

        } catch (IOException e) {
            log.error("[로컬] 파일 저장 실패", e);
            throw new MoplException(ErrorCode.LOCAL_FILE_SAVE_FAILED);
        }
    }

    // 허용 확장자 검증 후 확장자 반환
    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new MoplException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new MoplException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        return ext;
    }

    // ImageIO로 실제 바이트 디코딩 시도 — null이면 이미지가 아닌 파일
    private void validateImageContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new MoplException(ErrorCode.INVALID_IMAGE_FILE);
            }
        } catch (IOException e) {
            throw new MoplException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank())
            return;
        try {
            Path filePath = UPLOAD_DIR.resolve(Paths.get(fileUrl).getFileName());
            Files.deleteIfExists(filePath);
            log.info("[로컬] 파일 삭제: {}", fileUrl);
        } catch (IOException e) {
            log.error("[로컬] 파일 삭제 실패", e);
        }
    }

    // 로컬 저장 경로는 별도 key 개념이 없어 그대로 반환
    @Override
    public String extractKey(String fileUrl) {
        return fileUrl;
    }

    // 정적 리소스로 직접 서빙되므로 서명 없이 그대로 반환
    @Override
    public String getPresignedUrl(String key) {
        return key;
    }
}