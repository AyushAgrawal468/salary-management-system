package com.acme.salary;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Starts every test run from an empty schema so results never depend on leftovers. */
@TestConfiguration(proxyBeanMethods = false)
public class FlywayTestConfig {

    @Bean
    FlywayMigrationStrategy cleanMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
