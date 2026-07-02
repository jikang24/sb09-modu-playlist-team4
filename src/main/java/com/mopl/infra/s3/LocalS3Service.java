package com.mopl.infra.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@Profile("local")
public class LocalS3Service implements S3Service {

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        log.info("[로컬] S3 업로드 스킵: {}", file.getOriginalFilename());
        return "https://local-placeholder.s3.amazonaws.com/images/" + file.getOriginalFilename();
    }

    @Override
    public void delete(String fileUrl) {
        log.info("[로컬] S3 삭제 스킵: {}", fileUrl);
    }
}