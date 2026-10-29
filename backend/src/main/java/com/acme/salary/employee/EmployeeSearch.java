package com.acme.salary.employee;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional list filters; null fields are ignored. {@code q} is a case-insensitive
 * "contains" match on name, employee code or work email.
 */
public record EmployeeSearch(String q, String country, String department, String jobTitle, EmployeeStatus status) {

    private static final char ESCAPE = '!';

    // ponytail: leading-wildcard LIKE is a sequential scan; fine at 10k rows, add a pg_trgm index if it gets slow.
    Specification<Employee> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.strip().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, ESCAPE),
                        cb.like(cb.lower(root.get("employeeCode")), pattern, ESCAPE),
                        cb.like(cb.lower(root.get("workEmail")), pattern, ESCAPE)));
            }
            if (country != null) predicates.add(cb.equal(root.get("country"), country));
            if (department != null) predicates.add(cb.equal(root.get("department"), department));
            if (jobTitle != null) predicates.add(cb.equal(root.get("jobTitle"), jobTitle));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Users search for text, not patterns: "%" and "_" must match themselves. */
    private static String escapeLike(String s) {
        String esc = String.valueOf(ESCAPE);
        return s.replace(esc, esc + esc).replace("%", esc + "%").replace("_", esc + "_");
    }
}
