package com.dhairya.doodle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    private final String password;
    public SecurityConfig(@Value("${app.access-password:}") String password, @Value("${app.private-mode:false}") boolean privateMode) {
        if (privateMode && password.length() < 12) throw new IllegalStateException("Private mode requires GAME_ACCESS_PASSWORD with at least 12 characters.");
        this.password = password;
    }
    @Bean
    public SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        if (password.isBlank()) http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        else {
            http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/health", "/login", "/error").permitAll().anyRequest().authenticated());
            http.formLogin(login -> login.defaultSuccessUrl("/", true));
            http.logout(logout -> logout.logoutSuccessUrl("/login"));
        }
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean
    public UserDetailsService users() {
        // Local mode authorizes every request and does not expose a working default account.
        String value = password.isBlank() ? java.util.UUID.randomUUID().toString() : password;
        return new InMemoryUserDetailsManager(User.withUsername("host").password(passwordEncoder().encode(value)).roles("PLAYER").build());
    }
}
