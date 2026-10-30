package com.acme.salary.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds demo data on startup only when APP_SEED_ENABLED=true; never runs by default. With
 * APP_SEED_DEMO_PASSWORD (12+ characters) it also creates demo logins on a database with no users.
 */
@Component
@ConditionalOnBooleanProperty("app.seed.enabled")
class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final Seeder seeder;
    private final int employees;
    private final String demoPassword;

    SeedRunner(Seeder seeder, @Value("${app.seed.employees:10000}") int employees,
               @Value("${app.seed.demo-password:}") String demoPassword) {
        this.seeder = seeder;
        this.employees = employees;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        long start = System.currentTimeMillis();
        int inserted = seeder.seed(employees, LocalDate.now());
        if (inserted == 0) {
            log.info("Seed skipped: employees already exist");
        } else {
            log.info("Seeded {} employees in {} ms", inserted, System.currentTimeMillis() - start);
        }
        if (seeder.seedDemoUsers(demoPassword) > 0) {
            log.info("Created demo logins admin, hr and viewer");
        } else if (demoPassword.isBlank()) {
            log.info("No demo logins: APP_SEED_DEMO_PASSWORD is not set");
        }
    }
}
