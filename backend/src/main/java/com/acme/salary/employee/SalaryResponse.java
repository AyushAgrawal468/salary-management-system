package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalaryResponse(UUID id, BigDecimal amount, String currency, LocalDate effectiveDate) {

    static SalaryResponse from(SalaryRecord r) {
        return new SalaryResponse(r.getId(), r.getAmount(), r.getCurrency(), r.getEffectiveDate());
    }
}
