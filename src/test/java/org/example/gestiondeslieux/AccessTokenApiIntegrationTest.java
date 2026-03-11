package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AccessTokenApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void access_token_endpoints_should_support_create_list_get_patch_discover_and_revoke() throws Exception {
        AuthSession session = registerRandomUser("token");

        JsonNode createdPlace = postJson("/api/places", """
                {"title":"Token Place","description":"desc","latitude":48.85,"longitude":2.35,"tags":["token-test"]}
                """, session.accessToken(), status().isCreated());
        long placeId = unwrapApiResponseData(createdPlace).path("id").asLong(-1L);
        assertTrue(placeId > 0);

        JsonNode createdToken = postJson("/api/tokens", """
                {"resourceType":"PLACE","resourceId":%d,"permission":"READ","label":"token-test"}
                """.formatted(placeId), session.accessToken(), status().isCreated());
        JsonNode tokenData = unwrapApiResponseData(createdToken);
        long tokenId = tokenData.path("id").asLong(-1L);
        assertTrue(tokenId > 0);
        String token = requireTextField(tokenData, "token");

        mockMvc.perform(get("/api/tokens").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tokens/" + tokenId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        patchJson("/api/tokens/" + tokenId + "/expiration",
                "{\"expiresAt\":\"" + LocalDateTime.now().plusDays(1) + "\"}",
                session.accessToken(), status().isOk());

        JsonNode discover = getJson("/api/tokens/discover?token=" + token, null, status().isOk());
        JsonNode discoverData = unwrapApiResponseData(discover);
        assertEquals("PLACE", discoverData.path("resourceType").asText());
        assertEquals("READ", discoverData.path("permission").asText());
        assertEquals(placeId, discoverData.path("resourceId").asLong(-1L));
        assertTrue(discoverData.path("places").isArray());
        assertEquals(1, discoverData.path("places").size());
        assertEquals(placeId, discoverData.path("places").get(0).path("id").asLong(-1L));
        assertTrue(discoverData.path("expired").isBoolean());
        assertTrue(discoverData.path("revoked").isBoolean());

        mockMvc.perform(delete("/api/tokens/" + tokenId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void discover_should_return_collection_and_its_places_for_collection_token() throws Exception {
        AuthSession session = registerRandomUser("token-collection");
        String tag = "discover-" + UUID.randomUUID().toString().substring(0, 6);

        JsonNode createdPlace = postJson("/api/places", """
                {"title":"Discover Collection Place","description":"desc","latitude":48.85,"longitude":2.35,"tags":["%s"]}
                """.formatted(tag), session.accessToken(), status().isCreated());
        long placeId = unwrapApiResponseData(createdPlace).path("id").asLong(-1L);
        assertTrue(placeId > 0);

        JsonNode collections = getJson("/api/collections", session.accessToken(), status().isOk());
        long collectionId = findCollectionIdByTag(unwrapApiResponseData(collections), tag);
        assertTrue(collectionId > 0);

        JsonNode createdToken = postJson("/api/tokens", """
                {"resourceType":"COLLECTION","resourceId":%d,"permission":"READ","label":"discover-collection-token"}
                """.formatted(collectionId), session.accessToken(), status().isCreated());
        JsonNode tokenData = unwrapApiResponseData(createdToken);
        long tokenId = tokenData.path("id").asLong(-1L);
        String token = requireTextField(tokenData, "token");

        JsonNode discover = getJson("/api/tokens/discover?token=" + token, null, status().isOk());
        JsonNode discoverData = unwrapApiResponseData(discover);
        assertEquals("COLLECTION", discoverData.path("resourceType").asText());
        assertTrue(discoverData.path("collections").isArray());
        assertEquals(1, discoverData.path("collections").size());
        assertEquals(collectionId, discoverData.path("collections").get(0).path("id").asLong(-1L));
        assertTrue(discoverData.path("places").isArray());
        assertTrue(discoverData.path("places").size() >= 1);

        mockMvc.perform(delete("/api/tokens/" + tokenId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
    }
}
