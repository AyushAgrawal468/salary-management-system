package com.acme.salary.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Employee code is deliberately absent: it identifies the person across systems and never changes. */
public record UpdateEmployeeRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 255) String jobTitle,
        @NotBlank @Size(max = 255) String department,
        @NotNull @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 code, e.g. IN") String country,
        @NotBlank @Email @Size(max = 255) String workEmail,
        @Size(max = 50) String phone,
        @NotNull LocalDate hireDate,
        /** The version the client last read; a mismatch means someone else changed the record. */
        @NotNull Long version) {
}
