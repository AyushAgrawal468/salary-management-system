package com.acme.salary.employee;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DuplicateEmployeeCodeException extends ResponseStatusException {

    DuplicateEmployeeCodeException(String code) {
        super(HttpStatus.CONFLICT, "Employee code " + code + " already exists");
    }
}
