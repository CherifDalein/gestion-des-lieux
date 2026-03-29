package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PlaceApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void place_endpoints_should_support_crud_search_tags_share_and_images() throws Exception {
        AuthSession session = registerRandomUser("place");
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        JsonNode createdPlace = postJson("/api/places", """
                {"title":"Smoke Place %s","description":"desc","latitude":48.85,"longitude":2.35,"tags":["smoke","updated"]}
                """.formatted(suffix), session.accessToken(), status().isCreated());
        long placeId = unwrapApiResponseData(createdPlace).path("id").asLong(-1L);
        assertTrue(placeId > 0);

        mockMvc.perform(get("/api/places").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/places")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/places/" + placeId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        putJson("/api/places/" + placeId, """
                {"title":"Smoke Place Updated %s","description":"desc2","latitude":48.851,"longitude":2.351,"tags":["smoke","updated"]}
                """.formatted(suffix), session.accessToken(), status().isOk());

        mockMvc.perform(get("/api/places/search")
                        .param("q", "Smoke Place")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/places/nearby")
                        .param("lat", "48.85")
                        .param("lon", "2.35")
                        .param("radius", "10")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/places/tags").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/places/" + placeId + "/tags/newtag")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/places/" + placeId + "/tags/newtag")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        MockMultipartFile firstImage = new MockMultipartFile(
                "file", "img-1.txt", MediaType.TEXT_PLAIN_VALUE, "image-1".getBytes(StandardCharsets.UTF_8));
        JsonNode withImage = multipartPost("/api/places/" + placeId + "/image", firstImage,
                session.accessToken(), status().isOk());
        String imageUrl = requireTextField(unwrapApiResponseData(withImage), "imageUrl");
        long imageId = Long.parseLong(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
        mockMvc.perform(get("/api/images/" + imageId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/images/" + imageId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        MockMultipartFile secondImage = new MockMultipartFile(
                "file", "img-2.txt", MediaType.TEXT_PLAIN_VALUE, "image-2".getBytes(StandardCharsets.UTF_8));
        multipartPost("/api/places/" + placeId + "/image", secondImage, session.accessToken(), status().isOk());
        mockMvc.perform(delete("/api/places/" + placeId + "/image").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        JsonNode sharedPlace = postJson("/api/places/" + placeId + "/share",
                "{\"label\":\"share-place-test\"}", session.accessToken(), status().isOk());
        String placeShareToken = requireTextField(unwrapApiResponseData(sharedPlace), "token");
        mockMvc.perform(get("/api/places/" + placeId).param("token", placeShareToken))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(delete("/api/places/" + placeId).header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void collection_share_token_should_open_place_detail_and_image_when_place_belongs_to_collection() throws Exception {
        AuthSession session = registerRandomUser("collection-share");
        String sharedTag = "shared-" + UUID.randomUUID().toString().substring(0, 8);
        String otherTag = "other-" + UUID.randomUUID().toString().substring(0, 8);

        JsonNode sharedPlace = postJson("/api/places", """
                {"title":"Shared Collection Place","description":"desc","latitude":48.85,"longitude":2.35,"tags":["%s","extra"]}
                """.formatted(sharedTag), session.accessToken(), status().isCreated());
        long sharedPlaceId = unwrapApiResponseData(sharedPlace).path("id").asLong(-1L);
        assertTrue(sharedPlaceId > 0);

        JsonNode otherPlace = postJson("/api/places", """
                {"title":"Other Collection Place","description":"desc","latitude":48.86,"longitude":2.36,"tags":["%s"]}
                """.formatted(otherTag), session.accessToken(), status().isCreated());
        long otherPlaceId = unwrapApiResponseData(otherPlace).path("id").asLong(-1L);
        assertTrue(otherPlaceId > 0);

        MockMultipartFile image = new MockMultipartFile(
                "file", "shared-image.txt", MediaType.TEXT_PLAIN_VALUE, "image-from-collection".getBytes(StandardCharsets.UTF_8));
        JsonNode withImage = multipartPost("/api/places/" + sharedPlaceId + "/image", image,
                session.accessToken(), status().isOk());
        String imageUrl = requireTextField(unwrapApiResponseData(withImage), "imageUrl");
        long imageId = Long.parseLong(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));

        JsonNode collections = getJson("/api/collections", session.accessToken(), status().isOk());
        long sharedCollectionId = findCollectionIdByTag(unwrapApiResponseData(collections), sharedTag);
        long otherCollectionId = findCollectionIdByTag(unwrapApiResponseData(collections), otherTag);
        assertTrue(sharedCollectionId > 0);
        assertTrue(otherCollectionId > 0);

        JsonNode sharedCollection = postJson("/api/collections/" + sharedCollectionId + "/share",
                "{\"label\":\"shared-collection-token\"}", session.accessToken(), status().isOk());
        String sharedCollectionToken = requireTextField(unwrapApiResponseData(sharedCollection), "token");

        JsonNode otherCollection = postJson("/api/collections/" + otherCollectionId + "/share",
                "{\"label\":\"other-collection-token\"}", session.accessToken(), status().isOk());
        String otherCollectionToken = requireTextField(unwrapApiResponseData(otherCollection), "token");

        mockMvc.perform(get("/api/places/" + sharedPlaceId).param("token", sharedCollectionToken))
                .andExpect(status().isOk());

        MvcResult imageResult = mockMvc.perform(get("/api/images/" + imageId).param("token", sharedCollectionToken))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals("image-from-collection".getBytes(StandardCharsets.UTF_8),
                imageResult.getResponse().getContentAsByteArray());

        mockMvc.perform(get("/api/places/" + sharedPlaceId).param("token", otherCollectionToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/images/" + imageId).param("token", otherCollectionToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/places/" + otherPlaceId).param("token", sharedCollectionToken))
                .andExpect(status().isNotFound());
    }
}
