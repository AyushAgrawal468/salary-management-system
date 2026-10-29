package com.acme.salary.employee;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class StaleEmployeeException extends ResponseStatusException {

    public StaleEmployeeException() {
        super(HttpStatus.CONFLICT, "Employee was modified by someone else; reload and try again");
    }
}
