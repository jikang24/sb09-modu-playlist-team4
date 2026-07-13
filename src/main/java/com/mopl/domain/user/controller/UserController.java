package com.mopl.domain.user.controller;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "사용자 관리")
@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final S3Service s3Service;

    @Operation(summary = "사용자 등록(회원가입)")
    @PostMapping
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserCreateRequest request) {
        UserDto userDto = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @Operation(summary = "사용자 상세 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> find(@PathVariable UUID userId) {
        UserDto userDto = userService.find(userId);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @Operation(summary = "프로필 변경", description = "본인의 프로필만 변경할 수 있습니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable UUID userId,
            @RequestPart("request") @Valid UserUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal JwtClaims claims) {

        if (!claims.getUserId().equals(userId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }

        String imageUrl = s3Service.upload(image);
        UserDto userDto = userService.updateProfile(userId, request, imageUrl);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[어드민] 권한 수정")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDto> updateRole(@PathVariable UUID userId, @Valid @RequestBody UserRoleUpdateRequest request) {
        UserDto userDto = userService.updateRole(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @Operation(summary = "비밀번호 변경", description = "본인의 비밀번호만 변경할 수 있습니다.")
    @PatchMapping("/{userId}/password")
    public ResponseEntity<UserDto> updatePassword(@PathVariable UUID userId, @Valid @RequestBody ChangePasswordRequest request,
                                                  @AuthenticationPrincipal JwtClaims claims) {

        if (!claims.getUserId().equals(userId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }

        UserDto userDto = userService.updatePassword(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[어드민] 계정 잠금 상태 변경", description = "[어드민 기능] 계정 잠금 상태를 변경합니다.")
    @PatchMapping("/{userId}/locked")
    public ResponseEntity<UserDto> updateLocked(@PathVariable UUID userId, @RequestBody UserLockUpdateRequest request) {
        UserDto userDto = userService.updateLocked(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[어드민] 사용자 목록 조회(커서 페이지네이션)")
    @GetMapping
    public ResponseEntity<CursorPageResponse<UserDto>> getUsers(
            @RequestParam(required = false) String emailLike,
            @RequestParam(required = false) Role roleEqual,
            @RequestParam(required = false) Boolean isLocked,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam SortDirection sortDirection,
            @RequestParam UserSortBy sortBy
    ) {
        UserSearchRequest request = new UserSearchRequest(
                null ,emailLike, roleEqual, isLocked, cursor, idAfter,
                limit, sortDirection, sortBy
        );
        return ResponseEntity.ok(userService.findAll(request));
    }

}
