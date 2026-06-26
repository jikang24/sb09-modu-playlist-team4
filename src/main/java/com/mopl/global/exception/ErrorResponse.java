package com.mopl.global.exception;

public record ErrorResponse(
        String code, String message
) {
}
