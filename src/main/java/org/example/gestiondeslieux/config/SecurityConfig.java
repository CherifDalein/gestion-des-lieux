package org.example.gestiondeslieux.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF pour simplifier accès H2 console et API JSON (à ne pas faire en prod)
                .csrf(csrf -> csrf.disable())

                // Configurer les règles d'accès
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints publics
                        .requestMatchers(
                                "/h2-console/**",
                                "/api/users/register",
                                "/api/users/login"
                        ).permitAll()

                        // Endpoint GET public (exemple annonces)
                        .requestMatchers(HttpMethod.GET, "/annonces").permitAll()

                        // Toutes les autres requêtes nécessitent auth
                        .anyRequest().authenticated()
                )

                // Désactiver form login classique (on utilisera JWT pour API)
                .formLogin(form -> form.disable())

                // Autoriser l’accès à H2 console
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
