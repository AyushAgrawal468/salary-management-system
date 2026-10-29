package com.acme.salary.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record SeedEmployee(
        String employeeCode,
        String fullName,
        String jobTitle,
        String department,
        String country,
        String workEmail,
        String phone,
        LocalDate hireDate,
        LocalDate terminationDate,
        List<SeedSalary> salaries) {
}

record SeedSalary(BigDecimal amount, String currency, LocalDate effectiveDate) {
}
