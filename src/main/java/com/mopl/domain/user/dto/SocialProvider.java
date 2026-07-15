package com.mopl.domain.user.dto;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;

import java.util.Arrays;

public enum SocialProvider {
    GOOGLE, KAKAO;

    public static SocialProvider from(String registrationId) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new MoplException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));
    }
}
