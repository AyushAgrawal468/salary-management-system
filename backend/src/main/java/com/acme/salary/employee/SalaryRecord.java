package com.acme.salary.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One entry in an employee's salary history. Append-only: a raise is a new entry. */
@Entity
@Table(name = "salary_record")
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID employeeId;

    /** Annual base salary. */
    @Column(nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** ISO 4217 code, e.g. "INR". */
    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(nullable = false, updatable = false)
    private LocalDate effectiveDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SalaryRecord() {
        // for JPA
    }

    SalaryRecord(UUID employeeId, BigDecimal amount, String currency, LocalDate effectiveDate) {
        this.employeeId = employeeId;
        this.amount = amount;
        this.currency = currency;
        this.effectiveDate = effectiveDate;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public Instant getCreatedAt() { return createdAt; }
}
