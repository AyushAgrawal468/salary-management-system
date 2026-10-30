package com.acme.salary.seed;

import com.acme.salary.FlywayTestConfig;
import com.acme.salary.security.Role;
import com.acme.salary.security.UserAccount;
import com.acme.salary.security.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@Transactional
@Import(FlywayTestConfig.class)
class SeederTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Autowired
    Seeder seeder;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder encoder;

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

    @Test
    void createsOneDemoLoginPerRole() {
        assertThat(seeder.seedDemoUsers("demo-password-123")).isEqualTo(3);

        assertThat(users.findAll()).extracting(UserAccount::getUsername, UserAccount::getRole)
                .containsExactlyInAnyOrder(
                        tuple("admin", Role.ADMIN),
                        tuple("hr", Role.HR_MANAGER),
                        tuple("viewer", Role.VIEWER));
        assertThat(users.findAll()).allSatisfy(u -> {
            // Shared demo logins: forcing a change would lock every other reviewer out.
            assertThat(u.isMustChangePassword()).isFalse();
            assertThat(encoder.matches("demo-password-123", u.getPasswordHash())).isTrue();
        });
    }

    @Test
    void demoLoginsAreSkippedOnceAnyUserExists() {
        users.save(new UserAccount("real.admin", "{noop}x", Role.ADMIN, false));

        assertThat(seeder.seedDemoUsers("demo-password-123")).isZero();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void demoLoginsNeedAPasswordOfAtLeast12Characters() {
        assertThat(seeder.seedDemoUsers(null)).isZero();
        assertThat(seeder.seedDemoUsers("")).isZero();
        assertThat(seeder.seedDemoUsers("too-short")).isZero();
        assertThat(users.count()).isZero();
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
