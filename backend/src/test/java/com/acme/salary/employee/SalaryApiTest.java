package com.acme.salary.employee;

import com.acme.salary.FlywayTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(FlywayTestConfig.class)
class SalaryApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    EmployeeRepository employees;

    @Autowired
    EntityManager entityManager;

    String salaries;
    String employeeUrl;

    @BeforeEach
    void createEmployee() {
        Employee employee = employees.saveAndFlush(new Employee("EMP-0001", "Asha Rao", "Software Engineer",
                "Engineering", "IN", "asha.rao@acme.com", null, LocalDate.of(2022, 4, 1)));
        // Detach the seeded row so requests load it fresh, as they would in production.
        entityManager.clear();
        employeeUrl = "/api/employees/" + employee.getId();
        salaries = employeeUrl + "/salaries";
    }

    @Test
    void addsSalaryEntry() throws Exception {
        add(salaries, """
                {"amount":1850000.50,"currency":"INR","effectiveDate":"2022-04-01"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/salaries/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(1850000.50))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.effectiveDate").value("2022-04-01"));
    }

    @Test
    void listsHistoryNewestFirst() throws Exception {
        add(salaries, entry("2022-04-01", 1500000)).andExpect(status().isCreated());
        add(salaries, entry("2024-04-01", 1900000)).andExpect(status().isCreated());
        add(salaries, entry("2023-04-01", 1700000)).andExpect(status().isCreated());

        mvc.perform(get(salaries))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].effectiveDate", contains("2024-04-01", "2023-04-01", "2022-04-01")));
    }

    @Test
    void historyOfEmployeeWithoutSalaryIsEmpty() throws Exception {
        mvc.perform(get(salaries))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsInvalidEntry() throws Exception {
        add(salaries, """
                {"amount":-5,"currency":"inr"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.currency").exists())
                .andExpect(jsonPath("$.errors.effectiveDate").exists());
    }

    @Test
    void rejectsMoreThanTwoDecimals() throws Exception {
        add(salaries, """
                {"amount":100.555,"currency":"INR","effectiveDate":"2022-04-01"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void rejectsUnknownCurrency() throws Exception {
        add(salaries, """
                {"amount":100,"currency":"XYZ","effectiveDate":"2022-04-01"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown currency: XYZ"));
    }

    @Test
    void rejectsEffectiveDateBeforeHire() throws Exception {
        add(salaries, entry("2022-03-31", 100)).andExpect(status().isBadRequest());
    }

    @Test
    void secondEntryOnSameDateIs409() throws Exception {
        add(salaries, entry("2023-04-01", 100)).andExpect(status().isCreated());

        add(salaries, entry("2023-04-01", 200))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A salary entry already exists for 2023-04-01"));
    }

    @Test
    void unknownEmployeeIs404() throws Exception {
        String unknown = "/api/employees/" + UUID.randomUUID() + "/salaries";

        mvc.perform(get(unknown)).andExpect(status().isNotFound());
        add(unknown, entry("2023-04-01", 100)).andExpect(status().isNotFound());
    }

    @Test
    void addingSalaryBumpsEmployeeVersion() throws Exception {
        add(salaries, entry("2023-04-01", 100)).andExpect(status().isCreated());

        // A client holding version 0 now gets 409 instead of acting on a stale view.
        mvc.perform(get(employeeUrl)).andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void cannotTerminateBeforeExistingSalaryEntry() throws Exception {
        add(salaries, entry("2024-04-01", 100)).andExpect(status().isCreated());

        mvc.perform(post(employeeUrl + "/termination").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2024-03-31","version":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Employee has salary entries after 2024-03-31"));
    }

    @Test
    void cannotAddSalaryAfterTermination() throws Exception {
        mvc.perform(post(employeeUrl + "/termination").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"terminationDate":"2024-03-31","version":0}
                        """)).andExpect(status().isOk());

        add(salaries, entry("2024-04-01", 100))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Date is after termination date 2024-03-31"));
    }

    private static String entry(String date, long amount) {
        return """
                {"amount":%d,"currency":"INR","effectiveDate":"%s"}
                """.formatted(amount, date);
    }

    private ResultActions add(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
