package com.acme.salary.employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SalaryService {

    private final EmployeeRepository employees;
    private final SalaryRecordRepository salaries;
    private final EntityManager entityManager;

    SalaryService(EmployeeRepository employees, SalaryRecordRepository salaries, EntityManager entityManager) {
        this.employees = employees;
        this.salaries = salaries;
        this.entityManager = entityManager;
    }

    public SalaryRecord add(UUID employeeId, AddSalaryRequest request) {
        // Locks the employee and bumps its version: a concurrent termination waits, then fails its version check.
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        entityManager.lock(employee, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        requireKnownCurrency(request.currency());
        employee.requireEmployedOn(request.effectiveDate());
        if (salaries.existsByEmployeeIdAndEffectiveDate(employeeId, request.effectiveDate())) {
            throw duplicateDate(request.effectiveDate());
        }
        try {
            return salaries.saveAndFlush(new SalaryRecord(
                    employeeId, request.amount(), request.currency(), request.effectiveDate()));
        } catch (DataIntegrityViolationException e) {
            throw duplicateDate(request.effectiveDate());
        }
    }

    @Transactional(readOnly = true)
    public List<SalaryRecord> history(UUID employeeId) {
        if (!employees.existsById(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
        return salaries.findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
    }

    private static void requireKnownCurrency(String code) {
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency: " + code);
        }
    }

    private static ResponseStatusException duplicateDate(LocalDate date) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "A salary entry already exists for " + date);
    }
}
