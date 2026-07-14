package com.mopl.domain.user.domain;

import com.mopl.domain.user.dto.SocialProvider;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    public static SocialAccount of(User user, SocialProvider provider, String providerUserId) {
        SocialAccount account = new SocialAccount();
        account.user = user;
        account.provider = provider;
        account.providerUserId = providerUserId;
        return account;
    }
}
