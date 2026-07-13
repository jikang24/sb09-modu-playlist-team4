package com.mopl.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "social_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_account_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    public static SocialAccount of(User user, String provider, String providerUserId) {
        SocialAccount account = new SocialAccount();
        account.user = user;
        account.provider = provider;
        account.providerUserId = providerUserId;
        return account;
    }
}
