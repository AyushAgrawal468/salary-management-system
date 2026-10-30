package com.acme.salary.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

/**
 * Session-cookie auth for a same-origin SPA. Every /api/** call needs a logged-in user;
 * reads are open to all roles, changes to HR managers and admins. CSRF uses the cookie/header
 * pair Angular's HttpClient handles natively (XSRF-TOKEN cookie, X-XSRF-TOKEN header).
 */
@Configuration
class SecurityConfig implements WebMvcConfigurer {

    private final PasswordChangeGate passwordChangeGate;

    SecurityConfig(PasswordChangeGate passwordChangeGate) {
        this.passwordChangeGate = passwordChangeGate;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "HR_MANAGER", "VIEWER")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "HR_MANAGER")
                        // Static UI and /actuator/health (the only exposed actuator endpoint).
                        .anyRequest().permitAll())
                // Also issues the cookie on the SPA's first, unauthenticated call (see SpaCsrfFlowTest).
                .csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                // The default saves each rejected request in a new session to redirect back after
                // login; an SPA never uses that, and it would create a session per anonymous call.
                .requestCache(cache -> cache.disable())
                // Plain 401, no redirect and no WWW-Authenticate (which would pop the browser's login box).
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** BCrypt today; the "{id}" prefix in stored hashes lets the algorithm change later without a reset. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordChangeGate).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**");
    }
}
