package com.mopl.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 성공 응답 공통 포맷
 *
 * 에러 응답은 ErrorResponse가 담당, 성공 응답은 이 클래스가 담당
 * CursorPageResponse와 함께 data 필드에 담아서 반환 가능
 *
 * 사용 예시:
 *   return ResponseEntity.ok(ApiResponse.ok(contentDto));
 *   return ResponseEntity.status(201).body(ApiResponse.created(contentDto));
 *   return ResponseEntity.ok(ApiResponse.noContent());
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON 응답에서 제외
public record ApiResponse<T>(
    int status,
    String message,
    T data
) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(200, "success", data);
  }

  public static <T> ApiResponse<T> created(T data) {
    return new ApiResponse<>(201, "created", data);
  }

  public static ApiResponse<Void> noContent() {
    return new ApiResponse<>(204, "no description", null);
  }
}
