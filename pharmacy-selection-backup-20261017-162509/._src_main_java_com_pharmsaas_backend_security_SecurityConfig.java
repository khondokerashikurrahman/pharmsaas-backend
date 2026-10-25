package com.pharmsaas.backend.security;

import jakarta.servlet.DispatcherType;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        // Permit internal error dispatches only.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS, "/**"
                        ).permitAll()

                        // Public login
                        .requestMatchers(
                                HttpMethod.POST, "/api/auth/login"
                        ).permitAll()

                        // Public SaaS pharmacy registration
                        .requestMatchers(
                                HttpMethod.POST, "/api/public/registration"
                        ).permitAll()

                        // Generic user registration:
                        // Super Admin only
                        .requestMatchers(
                                HttpMethod.POST, "/api/auth"
                        ).hasRole("SUPER_ADMIN")

                        // Global user administration
                        .requestMatchers(
                                "/api/auth/users",
                                "/api/auth/users/**"
                        ).hasRole("SUPER_ADMIN")

                        // Read/update/delete user by ID
                        .requestMatchers(
                                "/api/auth/*"
                        ).hasRole("SUPER_ADMIN")

                        // Tenant membership endpoints.
                        // Controller also validates the user's tenant role.
                        .requestMatchers(
                                "/api/tenants/*/memberships",
                                "/api/tenants/*/memberships/**"
                        ).authenticated()

                        // Store endpoints.
                        // StoreService checks tenant membership and role.
                        .requestMatchers(
                                "/api/tenants/*/stores",
                                "/api/tenants/*/stores/**"
                        ).authenticated()

                        // Read shared medicine catalogue
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/medicines",
                                "/api/medicines/**"
                        ).authenticated()

                        // Modify shared medicine catalogue:
                        // Super Admin only
                        .requestMatchers(
                                "/api/medicines",
                                "/api/medicines/**"
                        ).hasRole("SUPER_ADMIN")

                        // Tenant administration
                        .requestMatchers(
                                "/api/tenants",
                                "/api/tenants/**"
                        ).hasRole("SUPER_ADMIN")

                        // Super Admin module
                        .requestMatchers(
                                "/api/super-admin",
                                "/api/super-admin/**"
                        ).hasRole("SUPER_ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:4200")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET", "POST", "PUT",
                        "PATCH", "DELETE", "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**", configuration
        );

        return source;
    }
}
