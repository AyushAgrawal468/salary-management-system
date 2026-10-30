package com.acme.salary.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Not @Transactional on purpose: a failed login must still commit its failure count even
 * though the request ends in an exception.
 */
@Service
class AuthService {

    /** BCrypt ignores everything past 72 bytes; refuse rather than silently truncate. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final Clock clock;

    AuthService(AuthenticationManager authenticationManager, UserAccountRepository users,
                PasswordEncoder encoder, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.encoder = encoder;
        this.clock = clock;
    }

    Authentication login(String rawUsername, String password) {
        String username = UserAccount.normalise(rawUsername);
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            users.findByUsername(username).ifPresent(u -> {
                u.recordSuccessfulLogin();
                users.save(u);
            });
            return auth;
        } catch (BadCredentialsException e) {
            // ponytail: read-modify-write, so parallel failures can undercount; an atomic UPDATE fixes that if it matters.
            users.findByUsername(username).ifPresent(u -> {
                u.recordFailedLogin(clock.instant());
                users.save(u);
            });
            throw invalidCredentials();
        } catch (AuthenticationException e) {
            // Locked or disabled: same answer as a wrong password, so nobody can probe which accounts exist.
            throw invalidCredentials();
        }
    }

    UserAccount user(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    void changePassword(String username, String currentPassword, String newPassword) {
        UserAccount user = user(username);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (newPassword.equals(currentPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the current one");
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is too long");
        }
        user.changePassword(encoder.encode(newPassword));
        users.save(user);
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
}
