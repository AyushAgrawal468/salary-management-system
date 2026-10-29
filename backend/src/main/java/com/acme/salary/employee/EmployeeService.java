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

    public Employee update(UUID id, UpdateEmployeeRequest request) {
        Employee employee = getAtVersion(id, request.version());
        employee.update(request);
        return repository.saveAndFlush(employee);
    }

    public Employee terminate(UUID id, TerminateEmployeeRequest request) {
        Employee employee = getAtVersion(id, request.version());
        employee.terminate(request.terminationDate());
        return repository.saveAndFlush(employee);
    }

    @Transactional(readOnly = true)
    public Employee get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Employee getAtVersion(UUID id, long expectedVersion) {
        Employee employee = get(id);
        if (employee.getVersion() != expectedVersion) {
            throw new StaleEmployeeException();
        }
        return employee;
    }
}
