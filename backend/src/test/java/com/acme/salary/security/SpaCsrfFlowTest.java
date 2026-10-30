package com.acme.salary.security;

import com.acme.salary.FlywayTestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CSRF handshake exactly as Angular performs it, with no test shortcuts. Runs in its own
 * application context because Spring Security's csrf() test helper, used by other tests,
 * replaces the token repository inside the shared context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Import(FlywayTestConfig.class)
class SpaCsrfFlowTest {

    static final String LOGIN = """
            {"username":"asha","password":"correct-horse-battery"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void createUser() {
        users.save(new UserAccount("asha", encoder.encode("correct-horse-battery"), Role.HR_MANAGER, false));
    }

    @AfterEach
    void deleteUsers() {
        users.deleteAll();
    }

    @Test
    void firstUnauthenticatedCallHandsOutReadableToken() throws Exception {
        var result = mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        Cookie token = result.getResponse().getCookie("XSRF-TOKEN");

        assertThat(token).isNotNull();
        assertThat(token.isHttpOnly()).as("Angular must be able to read it").isFalse();
        assertThat(result.getRequest().getSession(false))
                .as("anonymous calls must not create server-side sessions").isNull();
    }

    @Test
    void loginSucceedsWhenTokenIsEchoedInHeader() throws Exception {
        Cookie token = mvc.perform(get("/api/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/auth/login").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk());
    }

    @Test
    void loginFailsWithCookieButNoHeader() throws Exception {
        Cookie token = mvc.perform(get("/api/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        // What a forged cross-site form post looks like: the browser sends the cookie, but the
        // attacker's page cannot read it to put it in the header.
        mvc.perform(post("/api/auth/login").cookie(token)
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isForbidden());
    }
}
