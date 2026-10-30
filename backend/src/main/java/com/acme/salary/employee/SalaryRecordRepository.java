package com.acme.salary.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, UUID> {

    List<SalaryRecord> findByEmployeeIdOrderByEffectiveDateDesc(UUID employeeId);

    boolean existsByEmployeeIdAndEffectiveDate(UUID employeeId, LocalDate effectiveDate);

    boolean existsByEmployeeIdAndEffectiveDateAfter(UUID employeeId, LocalDate date);

    /** Per employee, the latest entry already in effect on {@code asOf}; future raises are ignored. */
    @Query(value = """
            SELECT DISTINCT ON (employee_id) *
            FROM salary_record
            WHERE employee_id IN (:employeeIds) AND effective_date <= :asOf
            ORDER BY employee_id, effective_date DESC
            """, nativeQuery = true)
    List<SalaryRecord> findCurrentFor(Collection<UUID> employeeIds, LocalDate asOf);
}
