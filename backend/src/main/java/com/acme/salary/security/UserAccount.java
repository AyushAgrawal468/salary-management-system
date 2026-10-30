package com.acme.salary.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccount {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean mustChangePassword;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private Instant lockedUntil;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {
        // for JPA
    }

    public UserAccount(String username, String passwordHash, Role role, boolean mustChangePassword) {
        this.username = normalise(username);
        this.passwordHash = passwordHash;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    /** Usernames are case-insensitive: "Asha" and "asha" are the same person. */
    static String normalise(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    void recordFailedLogin(Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plus(LOCK_DURATION);
            failedLoginAttempts = 0;
        }
    }

    void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}
