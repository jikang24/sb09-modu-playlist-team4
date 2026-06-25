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
    INVALID_PASSWORD("현재 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    //Content
    CONTENT_NOT_FOUND("콘텐츠를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CONTENT_ALREADY_EXISTS("이미 존재하는 콘텐츠입니다.", HttpStatus.CONFLICT),

    // Review 모듈
    REVIEW_NOT_FOUND("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS("이미 리뷰를 작성했습니다.", HttpStatus.CONFLICT),
    REVIEW_NOT_OWNER("본인의 리뷰만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),

    // Playlist 모듈
    PLAYLIST_NOT_FOUND("플레이리스트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PLAYLIST_NOT_OWNER("본인의 플레이리스트만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus status;
}
