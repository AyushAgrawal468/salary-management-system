package com.acme.salary.employee;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(FlywayTestConfig.class)
@WithMockUser(roles = "HR_MANAGER")
class EmployeeSearchApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    EmployeeRepository repository;

    @BeforeEach
    void seed() {
        Employee terminated = employee("EMP-004", "Dev Patel", "Sales Manager", "Sales", "IN");
        terminated.terminate(LocalDate.of(2024, 6, 30));
        repository.saveAllAndFlush(List.of(
                employee("EMP-001", "Asha Rao", "Software Engineer", "Engineering", "IN"),
                employee("EMP-002", "Ben Carter", "Software Engineer", "Engineering", "US"),
                employee("EMP-003", "Chen Wei", "Data Analyst", "Finance", "SG"),
                terminated,
                employee("EMP-005", "Ella Rao", "Recruiter", "People", "IN")));
    }

    @Test
    void listsAllSortedByNameWithPageMetadata() throws Exception {
        mvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].fullName",
                        contains("Asha Rao", "Ben Carter", "Chen Wei", "Dev Patel", "Ella Rao")))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void searchMatchesNameCaseInsensitively() throws Exception {
        mvc.perform(get("/api/employees").param("q", "rAo"))
                .andExpect(jsonPath("$.content[*].fullName", contains("Asha Rao", "Ella Rao")));
    }

    @Test
    void searchMatchesEmployeeCodeAndEmail() throws Exception {
        mvc.perform(get("/api/employees").param("q", "emp-003"))
                .andExpect(jsonPath("$.content[*].fullName", contains("Chen Wei")));
        mvc.perform(get("/api/employees").param("q", "ben.carter@"))
                .andExpect(jsonPath("$.content[*].fullName", contains("Ben Carter")));
    }

    @Test
    void searchTreatsWildcardsLiterally() throws Exception {
        mvc.perform(get("/api/employees").param("q", "%"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/employees").param("q", "_"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void filtersCombine() throws Exception {
        mvc.perform(get("/api/employees").param("country", "IN").param("status", "ACTIVE"))
                .andExpect(jsonPath("$.content[*].fullName", contains("Asha Rao", "Ella Rao")));
        mvc.perform(get("/api/employees").param("department", "Engineering").param("country", "US"))
                .andExpect(jsonPath("$.content[*].fullName", contains("Ben Carter")));
        mvc.perform(get("/api/employees").param("jobTitle", "Software Engineer"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void paginatesAndSorts() throws Exception {
        mvc.perform(get("/api/employees").param("size", "2").param("page", "1").param("sort", "fullName,desc"))
                .andExpect(jsonPath("$.content[*].fullName", contains("Chen Wei", "Ben Carter")))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        mvc.perform(get("/api/employees").param("size", "100000"))
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void unknownSortPropertyIs400() throws Exception {
        mvc.perform(get("/api/employees").param("sort", "salary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusIs400() throws Exception {
        mvc.perform(get("/api/employees").param("status", "RETIRED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("Invalid value: RETIRED"));
    }

    private static Employee employee(String code, String name, String title, String department, String country) {
        String email = name.toLowerCase().replace(' ', '.') + "@acme.com";
        return new Employee(code, name, title, department, country, email, null, LocalDate.of(2020, 1, 1));
    }
}
