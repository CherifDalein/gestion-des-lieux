package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserProfileApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void user_profile_endpoints_should_support_read_update_and_password_change() throws Exception {
        AuthSession session = registerRandomUser("profile");

        JsonNode me = getJson("/api/users/me", session.accessToken(), status().isOk());
        JsonNode meData = unwrapApiResponseData(me);
        assertNotNull(requireTextField(meData, "email"));

        String updatedEmail = "profile-updated-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        putJson("/api/users/me", """
                {"firstName":"Updated","lastName":"User","email":"%s"}
                """.formatted(updatedEmail), session.accessToken(), status().isOk());

        String newPassword = "password456";
        putJson("/api/users/me/password", """
                {"currentPassword":"%s","newPassword":"%s"}
                """.formatted(session.password(), newPassword), session.accessToken(), status().isNoContent());

        postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(updatedEmail, session.password()), status().isUnauthorized());

        JsonNode relogin = postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(updatedEmail, newPassword), status().isOk());
        assertNotNull(requireTextField(relogin, "accessToken"));
    }
}
