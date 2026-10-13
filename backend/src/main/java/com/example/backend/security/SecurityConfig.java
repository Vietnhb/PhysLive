package com.example.backend.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Central Spring Security configuration.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Stateless sessions – every request must carry a valid JWT.</li>
 *   <li>BCrypt password encoder registered as a Spring bean.</li>
 *   <li>CORS allowed origins read from {@code cors.allowed-origins} env variable.</li>
 *   <li>Fine-grained RBAC enforced at method level via {@code @PreAuthorize}
 *       ({@link EnableMethodSecurity} is enabled).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize / @PostAuthorize on service/controller methods
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /** Comma-separated list of allowed CORS origins, e.g. {@code http://localhost:5173}. */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    // ------------------------------------------------------------------ //
    // Password encoder
    // ------------------------------------------------------------------ //

    /**
     * BCrypt password encoder – strength 12 (good balance of security vs. speed).
     * Injected into {@link com.example.backend.service.AuthService} via constructor.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ------------------------------------------------------------------ //
    // HTTP security chain
    // ------------------------------------------------------------------ //

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/health",
                                "/actuator/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/schools/*/imports/**").hasAnyRole("SCHOOL", "MANAGER", "STAFF")
                        .requestMatchers("/api/schools/*/assignments/**").hasRole("STAFF")
                        .requestMatchers("/api/schools/*/classes/**").hasAnyRole("SCHOOL", "MANAGER", "STAFF")
                        .requestMatchers("/api/schools/**").hasAnyRole("SCHOOL", "MANAGER")
                        .requestMatchers("/api/school/**").hasRole("SCHOOL")
                        .requestMatchers("/api/reviewer/**").hasAnyRole("REVIEWER", "MANAGER")
                        .requestMatchers("/api/problems/**", "/api/specifications/**", "/api/validations/**")
                            .hasAnyRole("STAFF", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/simulations/**").hasAnyRole("STAFF", "MANAGER")
                        .requestMatchers("/api/assignments/**").hasAnyRole("STAFF", "STUDENT", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/module-releases/*/retire").hasRole("MANAGER")
                        .requestMatchers("/api/module-releases/**").hasAnyRole("REVIEWER", "MANAGER")
                        .requestMatchers("/api/user/me").authenticated()
                        .requestMatchers("/api/user/all").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ------------------------------------------------------------------ //
    // CORS
    // ------------------------------------------------------------------ //

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Support multiple origins declared in the env variable (comma-separated)
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
