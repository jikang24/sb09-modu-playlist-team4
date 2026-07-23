package com.mopl.domain.user.service;

import com.mopl.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 저장된 profileImageUrl/thumbnailUrl 값을 응답용 URL로 변환
// 값에 스킴("://")이 없으면 우리가 업로드해 저장한 S3 key이므로 presigned URL로 치환하고,
// 스킴이 있으면(소셜 로그인 프로필 이미지 등 외부 URL) 그대로 반환한다.
@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

    private final S3Service s3Service;

    public String resolve(String stored) {
        if (stored == null || stored.isBlank() || stored.contains("://")) {
            return stored;
        }
        return s3Service.getPresignedUrl(stored);
    }
}
