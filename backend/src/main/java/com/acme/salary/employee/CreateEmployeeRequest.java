package com.acme.salary.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 50) String employeeCode,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 255) String jobTitle,
        @NotBlank @Size(max = 255) String department,
        @NotNull @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 code, e.g. IN") String country,
        @NotBlank @Email @Size(max = 255) String workEmail,
        @Size(max = 50) String phone,
        @NotNull LocalDate hireDate) {

    Employee toEmployee() {
        return new Employee(employeeCode, fullName, jobTitle, department, country, workEmail, phone, hireDate);
    }
}
