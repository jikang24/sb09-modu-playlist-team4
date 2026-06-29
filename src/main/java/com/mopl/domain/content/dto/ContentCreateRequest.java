package com.mopl.domain.content.dto;

import com.mopl.domain.content.domain.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 콘텐츠 등록 요청 (multipart의 "request" 파트)
 * thumbnailUrl 없음 → MultipartFile로 별도 수신 후 S3 업로드
 */
public record ContentCreateRequest(

    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    ContentType type,

    @NotBlank(message = "외부 ID는 필수입니다.")
    String externalId,

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    String title,

    String description,

    List<String> tags
) {}
