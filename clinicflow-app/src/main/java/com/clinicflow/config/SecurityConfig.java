package com.clinicflow.config;

import com.clinicflow.security.AuditingAuthenticationFailureHandler;
import com.clinicflow.security.ClinicStatusFilter;
import com.clinicflow.security.LoginThrottleFilter;
import com.clinicflow.security.RoleBasedAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RoleBasedAuthenticationSuccessHandler successHandler;
    private final AuditingAuthenticationFailureHandler failureHandler;
    private final LoginThrottleFilter loginThrottleFilter;
    private final ClinicStatusFilter clinicStatusFilter;

    @Value("${clinicflow.security.remember-me-key}")
    private String rememberMeKey;

    public SecurityConfig(RoleBasedAuthenticationSuccessHandler successHandler,
                          AuditingAuthenticationFailureHandler failureHandler,
                          LoginThrottleFilter loginThrottleFilter,
                          ClinicStatusFilter clinicStatusFilter) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.loginThrottleFilter = loginThrottleFilter;
        this.clinicStatusFilter = clinicStatusFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login",
                                "/setup", "/setup/**",
                                "/invite", "/invite/**",
                                "/register", "/register/**",
                                "/verify-email", "/verify-email/**",
                                "/verify-email-change", "/verify-email-change/**",
                                "/forgot-password", "/forgot-password/**",
                                "/reset-password", "/reset-password/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/actuator/health", "/error", "/error/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/clinic/**").hasRole("CLINIC_ADMIN")
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/patient/**").hasRole("PATIENT")
                        .requestMatchers("/pharmacy/**").hasAnyRole("PHARMACY_ADMIN", "PHARMACIST")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                )
                .addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(clinicStatusFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}