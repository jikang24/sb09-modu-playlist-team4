package com.mopl.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //사용자 관련
    USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NAME("이미 사용 중인 이름입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD("비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    FORBIDDEN("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    LOCKED_ACCOUNT("잠긴 계정입니다.", HttpStatus.FORBIDDEN),

    //Content
    CONTENT_NOT_FOUND("콘텐츠를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CONTENT_ALREADY_EXISTS("이미 존재하는 콘텐츠입니다.", HttpStatus.CONFLICT),

    // Review 모듈
    REVIEW_NOT_FOUND("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS("이미 리뷰를 작성했습니다.", HttpStatus.CONFLICT),
    REVIEW_NOT_OWNER("본인의 리뷰만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),

    // Playlist 모듈
    PLAYLIST_NOT_FOUND("플레이리스트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PLAYLIST_NOT_OWNER("본인의 플레이리스트만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),


    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    USER_LOCKED("잠긴 계정입니다.", HttpStatus.FORBIDDEN),

    // JWT 토큰
    TOKEN_EXPIRED("만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("리프레시 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("이미 무효화된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),

    // 공통 응답
    INVALID_INPUT("잘못된 입력입니다", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;
}
