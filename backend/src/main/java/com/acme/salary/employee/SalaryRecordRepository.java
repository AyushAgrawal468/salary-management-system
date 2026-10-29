package com.acme.salary.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, UUID> {

    List<SalaryRecord> findByEmployeeIdOrderByEffectiveDateDesc(UUID employeeId);

    boolean existsByEmployeeIdAndEffectiveDate(UUID employeeId, LocalDate effectiveDate);

    boolean existsByEmployeeIdAndEffectiveDateAfter(UUID employeeId, LocalDate date);
}
