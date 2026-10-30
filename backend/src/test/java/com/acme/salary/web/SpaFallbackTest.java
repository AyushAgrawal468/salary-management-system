package com.acme.salary.web;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uses the stand-in UI in src/test/resources/static. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FlywayTestConfig.class)
class SpaFallbackTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rootServesTheApp() throws Exception {
        // Spring Boot's welcome page forwards "/" to index.html; MockMvc records forwards rather than following them.
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void deepLinkServesTheAppSoAngularCanRouteIt() throws Exception {
        mvc.perform(get("/employees/0b9f5c3e-1111-4c2a-9d1e-123456789abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spa-index-for-tests")));
    }

    @Test
    void existingAssetIsServedAsIs() throws Exception {
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("asset-for-tests")));
    }

    @Test
    void missingAssetIs404NotTheApp() throws Exception {
        // Returning HTML for a missing script would surface as a confusing JS syntax error.
        mvc.perform(get("/main-OLDHASH.js")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void unknownApiPathIs404NotTheApp() throws Exception {
        mvc.perform(get("/api/no-such-endpoint")).andExpect(status().isNotFound());
    }
}
