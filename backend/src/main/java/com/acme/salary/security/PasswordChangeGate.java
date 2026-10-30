package com.acme.salary.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Users holding a temporary password (new accounts, admin resets) may only reach /api/auth/**
 * until they set their own. Registered in {@link SecurityConfig}.
 */
@Component
class PasswordChangeGate implements HandlerInterceptor {

    private final UserAccountRepository users;

    PasswordChangeGate(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }
        users.findByUsername(auth.getName())
                .filter(UserAccount::isMustChangePassword)
                .ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password change required");
                });
        return true;
    }
}
