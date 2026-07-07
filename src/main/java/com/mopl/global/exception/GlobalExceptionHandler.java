package com.mopl.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MoplException.class)
    public ResponseEntity<ErrorResponse> handleMoplException(MoplException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }
    /**
     * @Valid 유효성 검사 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("잘못된 입력값입니다.");

        log.warn("[유효성 검사 실패] {}", message);

        return ResponseEntity
            .status(400)
            .body(new ErrorResponse("INVALID_INPUT", message));
    }

    /**
     * 필수 요청 파라미터 누락 (예: limit 없이 목록 조회)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
        MissingServletRequestParameterException e) {

        log.warn("[필수 파라미터 누락] {}", e.getParameterName());

        return ResponseEntity
            .status(400)
            .body(new ErrorResponse("INVALID_INPUT",
                "필수 파라미터가 누락되었습니다: " + e.getParameterName()));
    }

    /**
     * 요청 파라미터 타입/값 오류
     * - 경로/쿼리 파라미터의 타입 변환 실패 (예: UUID 자리에 문자열, enum에 없는 값)
     * - limit 음수/0 등 도메인상 말이 안 되는 값으로 인한 IllegalArgumentException
     *   (JPA/Hibernate를 거치면 InvalidDataAccessApiUsageException으로 감싸져서 올라옴)
     */
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class,
        InvalidDataAccessApiUsageException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestValue(Exception e) {
        log.warn("[잘못된 요청 파라미터] {}", e.getMessage());

        return ResponseEntity
            .status(400)
            .body(new ErrorResponse("INVALID_INPUT", "잘못된 요청 파라미터입니다."));
    }

    /**
     * @PreAuthorize 등 메서드 보안 실패
     * ExceptionTranslationFilter보다 @RestControllerAdvice가 먼저 처리하므로
     * 여기서 잡아주지 않으면 최후 안전망(Exception.class)이 500으로 감싸버림
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException e) {
        log.warn("[권한 거부] {}", e.getMessage());

        return ResponseEntity
            .status(403)
            .body(new ErrorResponse("FORBIDDEN", "권한이 없습니다."));
    }

    /**
     * 존재하지 않는 정적 리소스 요청 (예: 없는 이미지 경로)
     * 최후 안전망(Exception.class)이 잡으면 단순 404가 500으로 둔갑하므로 여기서 먼저 처리
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("[정적 리소스 없음] {}", e.getResourcePath());

        return ResponseEntity
            .status(404)
            .body(new ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
    }

    /**
    * 예상치 못한 서버 에러 처리 (최후의 안전망)
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[예상치 못한 오류] {}", e.getMessage(), e);

        return ResponseEntity
            .status(500)
            .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
