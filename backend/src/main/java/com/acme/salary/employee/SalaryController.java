package com.acme.salary.employee;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees/{employeeId}/salaries")
class SalaryController {

    private final SalaryService service;

    SalaryController(SalaryService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<SalaryResponse> add(@PathVariable UUID employeeId, @Valid @RequestBody AddSalaryRequest request) {
        SalaryRecord created = service.add(employeeId, request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(SalaryResponse.from(created));
    }

    @GetMapping
    List<SalaryResponse> history(@PathVariable UUID employeeId) {
        return service.history(employeeId).stream().map(SalaryResponse::from).toList();
    }
}
