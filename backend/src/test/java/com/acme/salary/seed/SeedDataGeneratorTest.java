package com.acme.salary.seed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDataGeneratorTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 30);
    static List<SeedEmployee> employees;

    @BeforeAll
    static void generate() {
        employees = new SeedDataGenerator(42).generate(10_000, TODAY);
    }

    @Test
    void generatesRequestedCount() {
        assertThat(employees).hasSize(10_000);
    }

    @Test
    void isDeterministicForSameSeed() {
        assertThat(new SeedDataGenerator(42).generate(50, TODAY))
                .isEqualTo(new SeedDataGenerator(42).generate(50, TODAY));
    }

    @Test
    void codesAndEmailsAreUnique() {
        assertThat(employees).extracting(SeedEmployee::employeeCode).doesNotHaveDuplicates();
        assertThat(employees).extracting(SeedEmployee::workEmail).doesNotHaveDuplicates();
    }

    @Test
    void fieldsAreValid() {
        assertThat(employees).allSatisfy(e -> {
            assertThat(e.country()).matches("[A-Z]{2}");
            assertThat(e.workEmail()).matches("[a-z.0-9]+@acme\\.com");
            assertThat(e.fullName()).isNotBlank();
            assertThat(e.jobTitle()).isNotBlank();
            assertThat(e.department()).isNotBlank();
            assertThat(e.hireDate()).isBefore(TODAY.plusDays(1)).isAfter(TODAY.minusYears(11));
        });
    }

    @Test
    void everyEmployeeHasSalaryHistoryWithinEmployment() {
        assertThat(employees).allSatisfy(e -> {
            assertThat(e.salaries()).isNotEmpty();
            assertThat(e.salaries().getFirst().effectiveDate()).isEqualTo(e.hireDate());
            LocalDate lastDay = e.terminationDate() != null ? e.terminationDate() : TODAY;
            assertThat(e.salaries()).allSatisfy(s -> {
                assertThat(s.effectiveDate()).isBetween(e.hireDate(), lastDay);
                assertThat(s.amount().signum()).isPositive();
                assertThat(s.amount().scale()).isEqualTo(2);
                assertThat(Currency.getInstance(s.currency())).isNotNull();
            });
        });
    }

    @Test
    void salaryHistoryIsChronologicalAndNeverDecreases() {
        assertThat(employees).allSatisfy(e -> {
            for (int i = 1; i < e.salaries().size(); i++) {
                assertThat(e.salaries().get(i).effectiveDate()).isAfter(e.salaries().get(i - 1).effectiveDate());
                assertThat(e.salaries().get(i).amount()).isGreaterThanOrEqualTo(e.salaries().get(i - 1).amount());
            }
        });
    }

    @Test
    void oneCurrencyPerEmployeeMatchingCountry() {
        assertThat(employees).allSatisfy(e ->
                assertThat(e.salaries()).extracting(SeedSalary::currency).containsOnly(e.salaries().getFirst().currency()));
        SeedEmployee indian = employees.stream().filter(e -> e.country().equals("IN")).findFirst().orElseThrow();
        assertThat(indian.salaries().getFirst().currency()).isEqualTo("INR");
    }

    @Test
    void spreadsAcrossCountriesAndDepartments() {
        Set<String> countries = Set.copyOf(employees.stream().map(SeedEmployee::country).toList());
        Set<String> departments = Set.copyOf(employees.stream().map(SeedEmployee::department).toList());
        assertThat(countries).hasSizeGreaterThanOrEqualTo(6);
        assertThat(departments).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void someButFewAreTerminated() {
        long terminated = employees.stream().filter(e -> e.terminationDate() != null).count();
        assertThat(terminated).isBetween(300L, 1_000L);
    }
}
