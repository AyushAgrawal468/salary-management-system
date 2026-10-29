package com.acme.salary.web;

import com.acme.salary.employee.StaleEmployeeException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders every API error as RFC 7807 problem+json. Spring's base class already covers
 * ResponseStatusException and malformed requests; this adds per-field validation errors.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        var problem = ex.getBody();
        problem.setDetail("Request has invalid fields");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Two saves raced past the version check; the loser gets the same answer as a stale version. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> handleConcurrentUpdate(OptimisticLockingFailureException ex, WebRequest request) {
        var stale = new StaleEmployeeException();
        return handleErrorResponseException(stale, stale.getHeaders(), stale.getStatusCode(), request);
    }
}
