package com.acme.salary.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first ADMIN from APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD when there are no users
 * at all. The password is temporary: the admin must replace it at first login, so the value
 * left in the host's environment stops being useful.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    AdminBootstrap(UserAccountRepository users, PasswordEncoder encoder,
                   @Value("${app.admin.username:}") String username,
                   @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (createFirstAdmin(username, password)) {
            log.info("Created first admin '{}'; they must change the password at first login", username);
        } else if (users.count() == 0) {
            log.warn("No users exist and APP_ADMIN_USERNAME/APP_ADMIN_PASSWORD are not set: nobody can log in");
        }
    }

    @Transactional
    public boolean createFirstAdmin(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank() || users.count() > 0) {
            return false;
        }
        users.save(new UserAccount(username, encoder.encode(password), Role.ADMIN, true));
        return true;
    }
}
