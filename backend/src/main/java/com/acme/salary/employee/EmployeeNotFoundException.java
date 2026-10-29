package com.acme.salary.employee;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

class EmployeeNotFoundException extends ResponseStatusException {

    EmployeeNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Employee " + id + " not found");
    }
}
