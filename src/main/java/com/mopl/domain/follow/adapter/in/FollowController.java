package com.mopl.domain.follow.adapter.in;

import com.mopl.domain.follow.application.port.in.FollowUserUseCase;
import com.mopl.domain.follow.application.port.in.GetFollowedByMeUseCase;
import com.mopl.domain.follow.application.port.in.GetFollowerCountUseCase;
import com.mopl.domain.follow.application.port.in.UnfollowUserUseCase;
import com.mopl.domain.follow.domain.Follow;
import com.mopl.domain.follow.dto.FollowDto;
import com.mopl.domain.follow.dto.FollowRequest;
import com.mopl.global.jwt.JwtClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "팔로우")
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@Slf4j
public class FollowController {

    private final FollowUserUseCase followUserUseCase;
    private final UnfollowUserUseCase unfollowUserUseCase;
    private final GetFollowedByMeUseCase getFollowedByMeUseCase;
    private final GetFollowerCountUseCase getFollowerCountUseCase;

    @Operation(summary = "팔로우")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 오류"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<FollowDto> follow(
        @RequestBody FollowRequest request,
        @AuthenticationPrincipal JwtClaims claims
    ) {
        UUID followerId = claims.getUserId();
        log.info("팔로우 요청 - followerId: {}, followeeId: {}", followerId, request.followeeId());
        Follow follow = followUserUseCase.follow(request.followeeId(), followerId);
        return ResponseEntity.ok(toDto(follow));
    }

    @Operation(summary = "팔로우 취소", description = "API 요청자 본인의 팔로우만 취소할 수 있습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 오류"),
        @ApiResponse(responseCode = "403", description = "권한 오류"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> unfollow(
        @PathVariable UUID followId,
        @AuthenticationPrincipal JwtClaims claims
    ) {
        UUID requesterId = claims.getUserId();
        log.info("팔로우 취소 요청 - followId: {}, requesterId: {}", followId, requesterId);
        unfollowUserUseCase.unfollow(followId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 유저를 내가 팔로우하는지 여부 조회",
        description = "팔로우 중이면 FollowDto(id 포함)를 반환합니다. 팔로우하지 않은 경우 404를 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 오류"),
        @ApiResponse(responseCode = "404", description = "해당 리소스 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/followed-by-me")
    public ResponseEntity<FollowDto> getFollowedByMe(
        @RequestParam UUID followeeId,
        @AuthenticationPrincipal JwtClaims claims
    ) {
        UUID myId = claims.getUserId();
        log.info("팔로우 여부 조회 - myId: {}, followeeId: {}", myId, followeeId);
        Follow follow = getFollowedByMeUseCase.getFollowedByMe(followeeId, myId);
        return ResponseEntity.ok(toDto(follow));
    }

    @Operation(summary = "특정 유저의 팔로워 수 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "401", description = "인증 오류")
    })
    @GetMapping("/count")
    public ResponseEntity<Long> countFollowers(@RequestParam UUID followeeId) {
        log.info("팔로워 수 조회 - followeeId: {}", followeeId);
        return ResponseEntity.ok(getFollowerCountUseCase.countFollowers(followeeId));
    }

    private FollowDto toDto(Follow follow) {
        return new FollowDto(
            follow.getId(),
            follow.getFolloweeId(),
            follow.getFollowerId()
        );
    }
}
