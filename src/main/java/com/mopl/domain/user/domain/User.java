
package com.mopl.domain.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "USER")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 60, nullable = true)
    private String password;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean locked = false;

    @CreatedDate
    @Column(columnDefinition = "timestamp with time zone", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    public void updateProfile(String name, String profileImageUrl) {
        if (name != null) this.name = name;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateLocked(boolean locked) {
        this.locked = locked;
    }
}