package com.acme.salary.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee")
public class Employee {

    // UUIDs keep IDs in URLs from revealing headcount or being guessable.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String employeeCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String department;

    /** ISO 3166-1 alpha-2 country code, e.g. "IN". */
    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private String workEmail;

    private String phone;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    private LocalDate terminationDate;

    // Optimistic locking: a stale concurrent edit fails instead of silently overwriting.
    @Version
    private long version;

    protected Employee() {
        // for JPA
    }

    public Employee(String employeeCode, String fullName, String jobTitle, String department,
                    String country, String workEmail, String phone, LocalDate hireDate) {
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.department = department;
        this.country = country;
        this.workEmail = workEmail;
        this.phone = phone;
        this.hireDate = hireDate;
    }

    void update(UpdateEmployeeRequest r) {
        this.fullName = r.fullName();
        this.jobTitle = r.jobTitle();
        this.department = r.department();
        this.country = r.country();
        this.workEmail = r.workEmail();
        this.phone = r.phone();
        this.hireDate = r.hireDate();
    }

    /** Records are kept after termination: pay history must stay reportable and auditable. */
    void terminate(LocalDate date) {
        if (status == EmployeeStatus.TERMINATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is already terminated");
        }
        if (date.isBefore(hireDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Termination date cannot be before hire date");
        }
        this.status = EmployeeStatus.TERMINATED;
        this.terminationDate = date;
    }

    public UUID getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getCountry() { return country; }
    public String getWorkEmail() { return workEmail; }
    public String getPhone() { return phone; }
    public LocalDate getHireDate() { return hireDate; }
    public EmployeeStatus getStatus() { return status; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public long getVersion() { return version; }
}
