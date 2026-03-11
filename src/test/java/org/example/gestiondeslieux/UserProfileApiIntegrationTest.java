package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                """.formatted(session.password(), newPassword), session.accessToken(), status().isOk());

        postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(updatedEmail, session.password()), status().isUnauthorized());

        JsonNode relogin = postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(updatedEmail, newPassword), status().isOk());
        assertNotNull(requireTextField(relogin, "accessToken"));
    }

    @Test
    void user_profile_stats_should_return_collection_place_and_shared_collection_counts() throws Exception {
        AuthSession session = registerRandomUser("stats");
        String tag = "stats-tag-" + UUID.randomUUID().toString().substring(0, 6);

        postJson("/api/places", """
                {"title":"Stats Place","description":"desc","latitude":48.85,"longitude":2.35,"tags":["%s"]}
                """.formatted(tag), session.accessToken(), status().isCreated());

        JsonNode collections = getJson("/api/collections", session.accessToken(), status().isOk());
        long collectionId = findCollectionIdByTag(unwrapApiResponseData(collections), tag);
        assertTrue(collectionId > 0);

        postJson("/api/collections/" + collectionId + "/share",
                "{\"label\":\"stats-share\"}", session.accessToken(), status().isOk());

        JsonNode stats = getJson("/api/users/me/stats", session.accessToken(), status().isOk());
        JsonNode data = unwrapApiResponseData(stats);

        assertEquals(1L, data.path("collectionCount").asLong(-1L));
        assertEquals(1L, data.path("ownedPlaceCount").asLong(-1L));
        assertEquals(1L, data.path("sharedCollectionCount").asLong(-1L));
    }
}
