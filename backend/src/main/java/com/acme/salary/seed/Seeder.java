package com.acme.salary.seed;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Writes generated data with plain JDBC batches in one transaction: all-or-nothing, and far
 * faster than going through JPA for tens of thousands of rows. Database constraints still apply.
 */
@Component
class Seeder {

    private static final long SEED = 42;

    private final JdbcTemplate jdbc;

    Seeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return employees inserted; 0 when the database already has employees. */
    @Transactional
    public int seed(int count, LocalDate today) {
        Long existing = jdbc.queryForObject("SELECT count(*) FROM employee", Long.class);
        if (existing != null && existing > 0) {
            return 0;
        }

        List<SeedEmployee> employees = new SeedDataGenerator(SEED).generate(count, today);
        List<Object[]> employeeRows = new ArrayList<>(employees.size());
        List<Object[]> salaryRows = new ArrayList<>();
        Timestamp now = Timestamp.from(Instant.now());
        for (SeedEmployee e : employees) {
            UUID id = UUID.randomUUID();
            employeeRows.add(new Object[]{id, e.employeeCode(), e.fullName(), e.jobTitle(), e.department(),
                    e.country(), e.workEmail(), e.phone(), Date.valueOf(e.hireDate()),
                    e.terminationDate() == null ? "ACTIVE" : "TERMINATED",
                    e.terminationDate() == null ? null : Date.valueOf(e.terminationDate())});
            for (SeedSalary s : e.salaries()) {
                salaryRows.add(new Object[]{UUID.randomUUID(), id, s.amount(), s.currency(),
                        Date.valueOf(s.effectiveDate()), now});
            }
        }

        jdbc.batchUpdate("""
                INSERT INTO employee (id, employee_code, full_name, job_title, department, country,
                                      work_email, phone, hire_date, status, termination_date, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, employeeRows);
        jdbc.batchUpdate("""
                INSERT INTO salary_record (id, employee_id, amount, currency, effective_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, salaryRows);
        return employees.size();
    }
}
