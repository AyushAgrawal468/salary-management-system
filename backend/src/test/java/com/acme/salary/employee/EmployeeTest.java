package com.acme.salary.employee;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTest {

    static final LocalDate HIRED = LocalDate.of(2022, 4, 1);

    @Test
    void terminateRecordsStatusAndDate() {
        Employee employee = employee();

        employee.terminate(LocalDate.of(2025, 1, 31));

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
        assertThat(employee.getTerminationDate()).isEqualTo(LocalDate.of(2025, 1, 31));
    }

    @Test
    void canBeTerminatedOnHireDate() {
        Employee employee = employee();

        employee.terminate(HIRED);

        assertThat(employee.getTerminationDate()).isEqualTo(HIRED);
    }

    @Test
    void terminationBeforeHireDateIsRejected() {
        Employee employee = employee();

        assertThatThrownBy(() -> employee.terminate(HIRED.minusDays(1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("before hire date")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void cannotTerminateTwice() {
        Employee employee = employee();
        employee.terminate(LocalDate.of(2025, 1, 31));

        assertThatThrownBy(() -> employee.terminate(LocalDate.of(2025, 2, 28)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already terminated")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(employee.getTerminationDate()).isEqualTo(LocalDate.of(2025, 1, 31));
    }

    @Test
    void isEmployedFromHireDateOnwards() {
        Employee employee = employee();

        employee.requireEmployedOn(HIRED);
        employee.requireEmployedOn(HIRED.plusYears(5));

        assertThatThrownBy(() -> employee.requireEmployedOn(HIRED.minusDays(1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("before hire date")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void isNotEmployedAfterTerminationDate() {
        Employee employee = employee();
        employee.terminate(LocalDate.of(2025, 1, 31));

        employee.requireEmployedOn(LocalDate.of(2025, 1, 31));

        assertThatThrownBy(() -> employee.requireEmployedOn(LocalDate.of(2025, 2, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("after termination date");
    }

    private static Employee employee() {
        return new Employee("EMP-0001", "Asha Rao", "Software Engineer", "Engineering", "IN",
                "asha.rao@acme.com", null, HIRED);
    }
}
