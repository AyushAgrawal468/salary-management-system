package com.acme.salary.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Seeds demo data on startup only when APP_SEED_ENABLED=true; never runs by default. */
@Component
@ConditionalOnBooleanProperty("app.seed.enabled")
class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final Seeder seeder;
    private final int employees;

    SeedRunner(Seeder seeder, @Value("${app.seed.employees:10000}") int employees) {
        this.seeder = seeder;
        this.employees = employees;
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
    }
}
