package com.tutormgmt.dev;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Opens {@code /dev/**} without authentication — that is the whole point of the dev sign-in.
 * Registered only under the {@code local} profile, ahead of the application's main chain, and
 * scoped to {@code /dev/**} so the rest of the API keeps its normal rules.
 */
@Profile("local")
@Configuration
public class DevSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/dev/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
