package com.acme.salary.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    @Test
    void newAccountIsNotLocked() {
        assertThat(account().isLocked(NOW)).isFalse();
    }

    @Test
    void locksAfterFiveFailedAttempts() {
        UserAccount account = account();
        for (int i = 0; i < 4; i++) {
            account.recordFailedLogin(NOW);
        }
        assertThat(account.isLocked(NOW)).isFalse();

        account.recordFailedLogin(NOW);

        assertThat(account.isLocked(NOW)).isTrue();
        assertThat(account.isLocked(NOW.plus(Duration.ofMinutes(14)))).isTrue();
    }

    @Test
    void lockExpiresAfterFifteenMinutes() {
        UserAccount account = lockedAccount();

        assertThat(account.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        UserAccount account = account();
        for (int i = 0; i < 4; i++) {
            account.recordFailedLogin(NOW);
        }

        account.recordSuccessfulLogin();
        account.recordFailedLogin(NOW);

        assertThat(account.isLocked(NOW)).isFalse();
    }

    @Test
    void changingPasswordClearsMustChangeFlag() {
        UserAccount account = new UserAccount("asha", "{bcrypt}old", Role.HR_MANAGER, true);

        account.changePassword("{bcrypt}new");

        assertThat(account.isMustChangePassword()).isFalse();
        assertThat(account.getPasswordHash()).isEqualTo("{bcrypt}new");
    }

    @Test
    void usernameIsNormalised() {
        assertThat(new UserAccount("  Asha.Rao ", "{bcrypt}x", Role.VIEWER, false).getUsername())
                .isEqualTo("asha.rao");
    }

    private static UserAccount account() {
        return new UserAccount("asha", "{bcrypt}hash", Role.HR_MANAGER, false);
    }

    private static UserAccount lockedAccount() {
        UserAccount account = account();
        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin(NOW);
        }
        return account;
    }
}
