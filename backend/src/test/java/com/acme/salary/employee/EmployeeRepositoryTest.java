package com.acme.salary.employee;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(FlywayTestConfig.class)
class EmployeeRepositoryTest {

    @Autowired
    EmployeeRepository repository;

    @Test
    void savesAndReloadsAllFields() {
        Employee saved = repository.saveAndFlush(employee("EMP-0001"));

        Employee found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(found.getFullName()).isEqualTo("Asha Rao");
        assertThat(found.getJobTitle()).isEqualTo("Software Engineer");
        assertThat(found.getDepartment()).isEqualTo("Engineering");
        assertThat(found.getCountry()).isEqualTo("IN");
        assertThat(found.getWorkEmail()).isEqualTo("asha.rao@acme.com");
        assertThat(found.getPhone()).isEqualTo("+91 98765 43210");
        assertThat(found.getHireDate()).isEqualTo(LocalDate.of(2022, 4, 1));
    }

    @Test
    void newEmployeeIsActive() {
        Employee saved = repository.saveAndFlush(employee("EMP-0002"));

        assertThat(saved.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void rejectsDuplicateEmployeeCode() {
        repository.saveAndFlush(employee("EMP-0003"));

        assertThatThrownBy(() -> repository.saveAndFlush(employee("EMP-0003")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsByEmployeeCode() {
        repository.saveAndFlush(employee("EMP-0004"));

        assertThat(repository.findByEmployeeCode("EMP-0004")).isPresent();
        assertThat(repository.findByEmployeeCode("EMP-9999")).isEmpty();
    }

    private static Employee employee(String code) {
        return new Employee(code, "Asha Rao", "Software Engineer", "Engineering", "IN",
                "asha.rao@acme.com", "+91 98765 43210", LocalDate.of(2022, 4, 1));
    }
}
