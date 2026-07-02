package com.mopl.domain.follow.adapter.out;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows",uniqueConstraints = {@UniqueConstraint(
        name = "uk_follower_followee",
        columnNames = {"follower_id", "followee_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowJpaEntity {
    @Id
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "followee_id", nullable = false)
    private UUID followeeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
