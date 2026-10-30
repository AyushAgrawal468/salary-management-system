-- Creates a login with a temporary password; the user must replace it at first login.
--
--   psql "<connection>" -v username=asha.rao -v role=HR_MANAGER -v password='<temporary password>' \
--        -f backend/scripts/create-user.sql
--
-- role: ADMIN | HR_MANAGER | VIEWER. The password is hashed here with BCrypt (pgcrypto) in the
-- same "{bcrypt}" format the app checks; the plain value is never stored.
\set ON_ERROR_STOP on

SELECT length(:'password') >= 12 AS password_ok \gset
\if :password_ok
\else
    \echo 'Password must be at least 12 characters.'
    \quit
\endif

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO app_user (id, username, password_hash, role, enabled, must_change_password,
                      failed_login_attempts, created_at)
VALUES (gen_random_uuid(),
        lower(trim(:'username')),
        '{bcrypt}' || crypt(:'password', gen_salt('bf', 10)),
        :'role',
        true,
        true,
        0,
        now())
RETURNING username, role, must_change_password;
