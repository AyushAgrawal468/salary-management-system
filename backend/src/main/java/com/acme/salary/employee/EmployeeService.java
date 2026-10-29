package com.acme.salary.employee;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;

    EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public Employee create(CreateEmployeeRequest request) {
        if (repository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateEmployeeCodeException(request.employeeCode());
        }
        try {
            return repository.saveAndFlush(request.toEmployee());
        } catch (DataIntegrityViolationException e) {
            // Another request inserted the same code between the check and the insert.
            throw new DuplicateEmployeeCodeException(request.employeeCode());
        }
    }

    @Transactional(readOnly = true)
    public Employee get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
