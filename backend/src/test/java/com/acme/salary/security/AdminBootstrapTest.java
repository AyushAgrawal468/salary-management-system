package com.acme.salary.security;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(FlywayTestConfig.class)
class AdminBootstrapTest {

    @Autowired
    AdminBootstrap bootstrap;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Test
    void createsFirstAdminWhoMustChangePassword() {
        assertThat(bootstrap.createFirstAdmin("admin", "initial-password-123")).isTrue();

        UserAccount admin = users.findByUsername("admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isMustChangePassword()).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo("initial-password-123");
        assertThat(encoder.matches("initial-password-123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void doesNothingOnceAnyUserExists() {
        bootstrap.createFirstAdmin("admin", "initial-password-123");

        assertThat(bootstrap.createFirstAdmin("other", "another-password-123")).isFalse();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void skipsWhenCredentialsAreMissing() {
        assertThat(bootstrap.createFirstAdmin("", "")).isFalse();
        assertThat(bootstrap.createFirstAdmin("admin", null)).isFalse();
        assertThat(users.count()).isZero();
    }
}
