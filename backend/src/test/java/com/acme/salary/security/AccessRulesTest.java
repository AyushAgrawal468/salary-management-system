package com.acme.salary.security;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(FlywayTestConfig.class)
class AccessRulesTest {

    static final String EMPLOYEE = """
            {"employeeCode":"EMP-0001","fullName":"Asha Rao","jobTitle":"Engineer","department":"Engineering",
             "country":"IN","workEmail":"asha@acme.com","hireDate":"2022-04-01"}
            """;

    @Autowired
    MockMvc mvc;

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                // A 401 must not trigger the browser's own Basic-auth popup.
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void healthAndUiArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        // Unknown non-API paths are the SPA's business: not found, never "log in first".
        mvc.perform(get("/employees/some-page")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isOk());
        createEmployee().andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void hrManagerCanWrite() throws Exception {
        createEmployee().andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanWrite() throws Exception {
        createEmployee().andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void writesWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void securityHeadersAreSet() throws Exception {
        mvc.perform(get("/api/employees"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    private ResultActions createEmployee() throws Exception {
        return mvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE));
    }
}
