CREATE TABLE app_user (
    id                    UUID         PRIMARY KEY,
    username              VARCHAR(100) NOT NULL CHECK (username = lower(username)),
    -- Delegating-encoder format, e.g. "{bcrypt}$2a$10$..."; never a plain password.
    password_hash         VARCHAR(255) NOT NULL,
    role                  VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'HR_MANAGER', 'VIEWER')),
    enabled               BOOLEAN      NOT NULL,
    must_change_password  BOOLEAN      NOT NULL,
    failed_login_attempts INTEGER      NOT NULL,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);
