package com.acme.salary.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Logout lives in SecurityConfig: Spring's LogoutFilter handles POST /api/auth/logout. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}

    record CurrentUser(String username, Role role, boolean mustChangePassword) {
        static CurrentUser from(UserAccount u) {
            return new CurrentUser(u.getUsername(), u.getRole(), u.isMustChangePassword());
        }
    }

    private final AuthService authService;
    private final SecurityContextRepository contextRepository;

    AuthController(AuthService authService, SecurityContextRepository contextRepository) {
        this.authService = authService;
        this.contextRepository = contextRepository;
    }

    @PostMapping("/login")
    CurrentUser login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authService.login(body.username(), body.password());
        // New session id on login, so a session id planted before login is worthless (session fixation).
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return CurrentUser.from(authService.user(auth.getName()));
    }

    @GetMapping("/me")
    CurrentUser me(Authentication auth) {
        return CurrentUser.from(authService.user(auth.getName()));
    }

    @PostMapping("/password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body, Authentication auth) {
        authService.changePassword(auth.getName(), body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
