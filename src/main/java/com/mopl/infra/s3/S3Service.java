package com.mopl.infra.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    String upload(MultipartFile file);

    void delete(String fileUrl);

    // 업로드 결과 URL(또는 이미 key인 값)에서 저장용 key만 추출
    String extractKey(String fileUrl);

    // key로 접근 가능한(만료되는) 조회용 URL 발급
    String getPresignedUrl(String key);
}