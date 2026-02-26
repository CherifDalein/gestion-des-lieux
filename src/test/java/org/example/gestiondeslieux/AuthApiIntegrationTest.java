package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void auth_endpoints_should_support_register_login_refresh_and_logout() throws Exception {
        AuthSession session = registerRandomUser("auth");
        assertNotNull(session.accessToken());
        assertNotNull(session.refreshToken());

        JsonNode login = postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(session.email(), session.password()), status().isOk());
        String accessToken = requireTextField(login, "accessToken");
        String refreshToken = requireTextField(login, "refreshToken");
        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        postJson("/api/auth/refresh",
                "{\"refreshToken\":\"" + refreshToken + "\"}",
                status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        postJson("/api/auth/login", """
                {"email":"%s","password":"wrong-password"}
                """.formatted(session.email()), status().isUnauthorized());
    }
}
