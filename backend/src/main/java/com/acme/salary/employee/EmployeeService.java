package com.acme.salary.employee;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;
    private final SalaryRecordRepository salaries;

    EmployeeService(EmployeeRepository repository, SalaryRecordRepository salaries) {
        this.repository = repository;
        this.salaries = salaries;
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
        if (salaries.existsByEmployeeIdAndEffectiveDateAfter(id, request.terminationDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Employee has salary entries after " + request.terminationDate());
        }
        employee.terminate(request.terminationDate());
        return repository.saveAndFlush(employee);
    }

    @Transactional(readOnly = true)
    public Page<Employee> search(EmployeeSearch search, Pageable pageable) {
        // Tie-break on id so rows with equal sort keys never shift between pages.
        Pageable stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
        return repository.findAll(search.toSpecification(), stable);
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
