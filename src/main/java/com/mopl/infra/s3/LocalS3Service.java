package com.mopl.infra.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
@Profile({"local","dev"})
public class LocalS3Service implements S3Service {
    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            Files.createDirectories(UPLOAD_DIR);
            String originalFilename = file.getOriginalFilename();
            String ext = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String fileName = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("[로컬] 파일 저장: {}", fileName);
            return "/uploads/" + fileName;

        } catch (IOException e) {
            log.error("[로컬] 파일 저장 실패", e);
            throw new RuntimeException("로컬 파일 저장 실패", e);
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
}