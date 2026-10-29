package com.acme.salary.employee;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
class EmployeeApiTest {

    static final String VALID = """
            {"employeeCode":"EMP-0001","fullName":"Asha Rao","jobTitle":"Software Engineer",
             "department":"Engineering","country":"IN","workEmail":"asha.rao@acme.com",
             "phone":"+91 98765 43210","hireDate":"2022-04-01"}
            """;

    @Autowired
    MockMvc mvc;

    @Test
    void createsEmployee() throws Exception {
        create(VALID)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/employees/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.employeeCode").value("EMP-0001"))
                .andExpect(jsonPath("$.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.country").value("IN"))
                .andExpect(jsonPath("$.hireDate").value("2022-04-01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void fetchesCreatedEmployee() throws Exception {
        String location = create(VALID).andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode").value("EMP-0001"))
                .andExpect(jsonPath("$.workEmail").value("asha.rao@acme.com"));
    }

    @Test
    void unknownEmployeeIs404Problem() throws Exception {
        mvc.perform(get("/api/employees/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void duplicateEmployeeCodeIs409() throws Exception {
        create(VALID).andExpect(status().isCreated());

        create(VALID)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Employee code EMP-0001 already exists"));
    }

    @Test
    void rejectsInvalidInputWithFieldErrors() throws Exception {
        create("""
                {"employeeCode":" ","fullName":"","jobTitle":"Engineer","department":"Engineering",
                 "country":"india","workEmail":"not-an-email","hireDate":null}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.errors.employeeCode").exists())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.country").exists())
                .andExpect(jsonPath("$.errors.workEmail").exists())
                .andExpect(jsonPath("$.errors.hireDate").exists());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        create("{not json").andExpect(status().isBadRequest());
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
