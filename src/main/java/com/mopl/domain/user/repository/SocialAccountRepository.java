package com.mopl.domain.user.repository;

import com.mopl.domain.user.domain.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    @Query("select sa.user.id from SocialAccount sa " +
            "where sa.provider = :provider and sa.providerUserId = :providerUserId")
    Optional<UUID> findUserIdByProviderAndProviderUserId(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId);}