package com.mopl.global.security.userdetails;

import com.mopl.global.auth.UserAuthInfo;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class MoplUserDetails implements UserDetails {

    private final UserAuthInfo userAuthInfo;

    public MoplUserDetails(UserAuthInfo userAuthInfo) {
        this.userAuthInfo = userAuthInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userAuthInfo.role().name()));
    }

    @Override
    public String getPassword() {
        return userAuthInfo.password();
    }

    @Override
    public String getUsername() {
        return userAuthInfo.email();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !userAuthInfo.locked();
    }
}
