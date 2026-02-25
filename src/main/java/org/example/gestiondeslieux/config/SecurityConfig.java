package org.example.gestiondeslieux.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gestiondeslieux.security.AccessTokenAuthFilter;
import org.example.gestiondeslieux.security.JwtAuthenticationFilter;
import org.example.gestiondeslieux.security.JwtUtil;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final IAccessTokenService accessTokenService;
    private final RateLimitConfig.RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtUtil jwtUtil,
                          IAccessTokenService accessTokenService,
                          RateLimitConfig.RateLimitFilter rateLimitFilter) {
        this.jwtUtil = jwtUtil;
        this.accessTokenService = accessTokenService;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeShortError(response, 401, "UNAUTHORIZED", "Authentification requise"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeShortError(response, 403, "FORBIDDEN", "Acces refuse"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/location/public/**").permitAll()
                .requestMatchers("/api/tokens/discover").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(
                new AccessTokenAuthFilter(accessTokenService),
                JwtAuthenticationFilter.class)
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeShortError(jakarta.servlet.http.HttpServletResponse response,
                                 int status,
                                 String error,
                                 String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String payload = new ObjectMapper().writeValueAsString(Map.of(
                "error", error,
                "message", message
        ));
        response.getWriter().write(payload);
    }
}
