package com.acme.salary.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;
    private final Clock clock;

    AccountUserDetailsService(UserAccountRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(UserAccount.normalise(username))
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .accountLocked(u.isLocked(clock.instant()))
                        .disabled(!u.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
