package com.mopl.domain.follow.application.service;

import com.mopl.domain.follow.application.port.in.FollowUserUseCase;
import com.mopl.domain.follow.application.port.in.GetFollowedByMeUseCase;
import com.mopl.domain.follow.application.port.in.GetFollowerCountUseCase;
import com.mopl.domain.follow.application.port.in.UnfollowUserUseCase;
import com.mopl.domain.follow.application.port.out.DeleteFollowPort;
import com.mopl.domain.follow.application.port.out.LoadFollowPort;
import com.mopl.domain.follow.application.port.out.SaveFollowPort;
import com.mopl.domain.follow.domain.Follow;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FollowService implements FollowUserUseCase, UnfollowUserUseCase,
    GetFollowedByMeUseCase, GetFollowerCountUseCase {

    private final SaveFollowPort saveFollowPort;
    private final LoadFollowPort loadFollowPort;
    private final DeleteFollowPort deleteFollowPort;

    @Override
    public Follow follow(UUID followeeId, UUID followerId) {
        if (loadFollowPort.existsByFolloweeIdAndFollowerId(followeeId, followerId)) {
            throw new MoplException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }
        Follow follow = Follow.create(followeeId, followerId);
        log.info("팔로우 생성 - followeeId: {}, followerId: {}", followeeId, followerId);
        return saveFollowPort.save(follow);
    }

    @Override
    public void unfollow(UUID followId, UUID followerId) {
        Follow follow = loadFollowPort.findById(followId)
            .orElseThrow(() -> new MoplException(ErrorCode.FOLLOW_NOT_FOUND));
        if (!follow.getFollowerId().equals(followerId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }
        log.info("팔로우 삭제 - followId: {}, requesterId: {}", followId, followerId);
        deleteFollowPort.deleteById(followId);
    }

    @Override
    @Transactional(readOnly = true)
    public Follow getFollowedByMe(UUID followeeId, UUID followerId) {
        return loadFollowPort.findByFolloweeIdAndFollowerId(followeeId, followerId)
            .orElseThrow(() -> new MoplException(ErrorCode.FOLLOW_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowers(UUID followeeId) {
        return loadFollowPort.countByFolloweeId(followeeId);
    }

}
