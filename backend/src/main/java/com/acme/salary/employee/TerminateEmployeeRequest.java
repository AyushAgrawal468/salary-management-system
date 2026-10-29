package com.acme.salary.employee;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TerminateEmployeeRequest(@NotNull LocalDate terminationDate, @NotNull Long version) {
}
