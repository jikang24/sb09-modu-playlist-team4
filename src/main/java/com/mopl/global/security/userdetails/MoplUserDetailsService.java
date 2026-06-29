package com.mopl.global.security.userdetails;

import com.mopl.global.auth.UserAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoplUserDetailsService implements UserDetailsService {

    private final UserAuthPort userAuthPort;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userAuthPort.findByEmail(email)
                .map(MoplUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
