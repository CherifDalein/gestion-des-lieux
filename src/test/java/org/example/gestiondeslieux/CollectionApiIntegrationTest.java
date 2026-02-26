package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CollectionApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void collection_endpoints_should_support_read_export_share_and_reject_unmapped_methods() throws Exception {
        AuthSession session = registerRandomUser("collection");
        String tag = "updated-" + UUID.randomUUID().toString().substring(0, 6);

        JsonNode createdPlace = postJson("/api/places", """
                {"title":"Collection Place","description":"desc","latitude":48.85,"longitude":2.35,"tags":["smoke","%s"]}
                """.formatted(tag), session.accessToken(), status().isCreated());
        long placeId = unwrapApiResponseData(createdPlace).path("id").asLong(-1L);
        assertTrue(placeId > 0);

        JsonNode collections = getJson("/api/collections", session.accessToken(), status().isOk());
        long collectionId = findCollectionIdByTag(unwrapApiResponseData(collections), tag);
        assertTrue(collectionId > 0);

        mockMvc.perform(post("/api/collections")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"manual\",\"tagFilter\":\"manual\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/collections/" + collectionId)
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"manual\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/collections/" + collectionId)
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/api/collections/" + collectionId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/collections/" + collectionId + "/places")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/collections/" + collectionId + "/export")
                        .param("format", "geojson")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        JsonNode sharedCollection = postJson("/api/collections/" + collectionId + "/share",
                "{\"label\":\"share-collection-test\"}", session.accessToken(), status().isOk());
        String shareToken = requireTextField(unwrapApiResponseData(sharedCollection), "token");
        mockMvc.perform(get("/api/collections/" + collectionId).param("token", shareToken))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/collections/" + collectionId + "/places").param("token", shareToken))
                .andExpect(status().is2xxSuccessful());
    }
}
