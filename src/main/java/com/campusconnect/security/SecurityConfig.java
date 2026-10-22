package com.campusconnect.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CampusUserDetailsService userDetailsService;

    public SecurityConfig(CampusUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Allow CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Static frontend files
                .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/*.ico", "/static/**").permitAll()

                // Public event discovery endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/*").permitAll()

                // Organizer-only event management
                .requestMatchers(HttpMethod.POST, "/api/v1/events").hasRole("ORGANIZER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/events/*").hasRole("ORGANIZER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/events/*").hasRole("ORGANIZER")

                // Student-only registration actions
                .requestMatchers(HttpMethod.POST, "/api/v1/registrations").hasRole("STUDENT")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/registrations/*").hasRole("STUDENT")

                // Authenticated endpoints (current user info, participant lists, student registrations)
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/events/*/registrations").authenticated()
                .requestMatchers("/api/v1/students/*/registrations").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: " + authException.getMessage() + "\"}");
            }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
