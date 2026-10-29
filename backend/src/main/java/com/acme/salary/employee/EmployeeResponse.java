package com.acme.salary.employee;

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
        long version) {

    static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getJobTitle(),
                e.getDepartment(), e.getCountry(), e.getWorkEmail(), e.getPhone(), e.getHireDate(),
                e.getStatus(), e.getTerminationDate(), e.getVersion());
    }
}
