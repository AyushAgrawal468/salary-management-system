package com.acme.salary.seed;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(FlywayTestConfig.class)
class SeederTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Autowired
    Seeder seeder;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void insertsEmployeesAndSalaryHistory() {
        int inserted = seeder.seed(200, TODAY);

        assertThat(inserted).isEqualTo(200);
        assertThat(count("employee")).isEqualTo(200);
        assertThat(count("salary_record")).isGreaterThanOrEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM employee WHERE status = 'TERMINATED'", Long.class))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM employee WHERE termination_date IS NOT NULL", Long.class));
    }

    @Test
    void skipsWhenEmployeesAlreadyExist() {
        seeder.seed(10, TODAY);

        int inserted = seeder.seed(10, TODAY);

        assertThat(inserted).isZero();
        assertThat(count("employee")).isEqualTo(10);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
