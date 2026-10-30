package com.acme.salary.security;

import com.acme.salary.FlywayTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deliberately not @Transactional: failed-login counters must survive the request that
 * throws, so these tests see what really gets committed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FlywayTestConfig.class)
class AuthApiTest {

    static final String PASSWORD = "correct-horse-battery";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserAccountRepository users;

    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void createUsers() {
        users.save(new UserAccount("asha", encoder.encode(PASSWORD), Role.HR_MANAGER, false));
        users.save(new UserAccount("newbie", encoder.encode(PASSWORD), Role.VIEWER, true));
    }

    @AfterEach
    void deleteUsers() {
        users.deleteAll();
    }

    @Test
    void loginReturnsUserAndStartsSession() throws Exception {
        var result = login("asha", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("asha"))
                .andExpect(jsonPath("$.role").value("HR_MANAGER"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("asha"));
        mvc.perform(get("/api/employees").session(session)).andExpect(status().isOk());
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        login("  ASHA ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        login("asha", "wrong-password-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        login("nobody", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void fiveFailuresLockTheAccountEvenForTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("asha", "wrong-password-123").andExpect(status().isUnauthorized());
        }

        login("asha", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        assertThat(users.findByUsername("asha").orElseThrow().isLocked(java.time.Instant.now())).isTrue();
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("asha", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginChangesSessionId() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String oldId = before.getId();

        var session = mvc.perform(post("/api/auth/login").session(before).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(credentials("asha", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        assertThat(session.getId()).isNotEqualTo(oldId);
    }

    @Test
    void logoutEndsSession() throws Exception {
        MockHttpSession session = sessionFor("asha");

        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithoutSessionIs401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void userWhoMustChangePasswordIsBlockedUntilTheyDo() throws Exception {
        MockHttpSession session = sessionFor("newbie");

        mvc.perform(get("/api/employees").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Password change required"));

        changePassword(session, PASSWORD, "a-brand-new-passphrase").andExpect(status().isNoContent());

        mvc.perform(get("/api/employees").session(session)).andExpect(status().isOk());
        login("newbie", "a-brand-new-passphrase")
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void passwordChangeChecksCurrentPassword() throws Exception {
        changePassword(sessionFor("asha"), "not-my-password", "a-brand-new-passphrase")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
    }

    @Test
    void passwordChangeEnforcesPolicy() throws Exception {
        MockHttpSession session = sessionFor("asha");

        changePassword(session, PASSWORD, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
        changePassword(session, PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("New password must differ from the current one"));
    }

    private MockHttpSession sessionFor(String username) throws Exception {
        return (MockHttpSession) login(username, PASSWORD).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(credentials(username, password)));
    }

    private ResultActions changePassword(MockHttpSession session, String current, String next) throws Exception {
        return mvc.perform(post("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"%s","newPassword":"%s"}
                        """.formatted(current, next)));
    }

    private static String credentials(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }
}
