package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String fullName,
        String jobTitle,
        String department,
        String country,
        String workEmail,
        String phone,
        LocalDate hireDate,
        EmployeeStatus status,
        LocalDate terminationDate,
        CurrentSalary currentSalary,
        long version) {

    /** Annual base salary in effect today; null when none has been recorded yet. */
    public record CurrentSalary(BigDecimal amount, String currency, LocalDate effectiveDate) {}

    static EmployeeResponse from(Employee e, SalaryRecord current) {
        CurrentSalary salary = current == null ? null
                : new CurrentSalary(current.getAmount(), current.getCurrency(), current.getEffectiveDate());
        return new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getJobTitle(),
                e.getDepartment(), e.getCountry(), e.getWorkEmail(), e.getPhone(), e.getHireDate(),
                e.getStatus(), e.getTerminationDate(), salary, e.getVersion());
    }
}
