package com.mopl.global.config;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트가 요청하는 콘텐츠 썸네일 fallback 이미지
 *
 * FE코드에 placeholder-movie.png가 없어 정적 리소스로 못 두는 상태.
 * 정적 리소스 핸들러를 그대로 타게 두면 NoResourceFoundException -> GlobalExceptionHandler가
 * JSON 404(application/json)를 응답해 <img> 태그가 깨져 보이므로, 이 경로만 컨트롤러로 먼저
 * 가로채 인라인 SVG를 200으로 내려준다. (정적 자산 배포 파이프라인에 대한 의존 없음)
 */
@Tag(name = "이미지")
@RestController
public class PlaceholderImageController {

  public static final String PATH = "/placeholder-movie.png";

  private static final byte[] FALLBACK_THUMBNAIL = """
      <svg xmlns="http://www.w3.org/2000/svg" width="300" height="450">
        <rect width="100%" height="100%" fill="#2a2a2a"/>
      </svg>
      """.getBytes(StandardCharsets.UTF_8);

  @GetMapping(value = PATH, produces = "image/svg+xml")
  public ResponseEntity<byte[]> placeholderMovie() {
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf("image/svg+xml"))
        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
        .body(FALLBACK_THUMBNAIL);
  }
}