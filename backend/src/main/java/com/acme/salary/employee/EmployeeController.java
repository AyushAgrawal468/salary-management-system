package com.acme.salary.employee;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
class EmployeeController {

    private final EmployeeService service;
    private final SalaryService salaries;

    EmployeeController(EmployeeService service, SalaryService salaries) {
        this.service = service;
        this.salaries = salaries;
    }

    @GetMapping
    Page<EmployeeResponse> list(EmployeeSearch search, @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        Page<Employee> page = service.search(search, pageable);
        Map<UUID, SalaryRecord> current = salaries.currentSalaries(page.map(Employee::getId).getContent());
        return page.map(e -> EmployeeResponse.from(e, current.get(e.getId())));
    }

    @GetMapping("/filters")
    EmployeeFilters filters() {
        return service.filters();
    }

    @PostMapping
    ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee created = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(respond(created));
    }

    @GetMapping("/{id}")
    EmployeeResponse get(@PathVariable UUID id) {
        return respond(service.get(id));
    }

    @PutMapping("/{id}")
    EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return respond(service.update(id, request));
    }

    @PostMapping("/{id}/termination")
    EmployeeResponse terminate(@PathVariable UUID id, @Valid @RequestBody TerminateEmployeeRequest request) {
        return respond(service.terminate(id, request));
    }

    private EmployeeResponse respond(Employee employee) {
        return EmployeeResponse.from(employee, salaries.currentSalaries(List.of(employee.getId())).get(employee.getId()));
    }
}
