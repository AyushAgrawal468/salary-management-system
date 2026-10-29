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

import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
class EmployeeController {

    private final EmployeeService service;

    EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    Page<EmployeeResponse> list(EmployeeSearch search, @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return service.search(search, pageable).map(EmployeeResponse::from);
    }

    @PostMapping
    ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee created = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(EmployeeResponse.from(created));
    }

    @GetMapping("/{id}")
    EmployeeResponse get(@PathVariable UUID id) {
        return EmployeeResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return EmployeeResponse.from(service.update(id, request));
    }

    @PostMapping("/{id}/termination")
    EmployeeResponse terminate(@PathVariable UUID id, @Valid @RequestBody TerminateEmployeeRequest request) {
        return EmployeeResponse.from(service.terminate(id, request));
    }
}
