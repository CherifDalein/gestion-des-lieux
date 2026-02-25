package org.example.gestiondeslieux;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApiEndpointsSmokeTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void should_cover_all_api_endpoints_without_server_errors() throws Exception {
        JsonNode login = postJson("/api/auth/login", """
                {"username":"alice","password":"password123"}
                """, status().isOk());
        String accessToken = requireTextField(login, "accessToken");
        String refreshToken = requireTextField(login, "refreshToken");
        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        postJson("/api/auth/refresh",
                "{\"refreshToken\":\"" + refreshToken + "\"}",
                status().isOk());

        String random = UUID.randomUUID().toString().substring(0, 8);
        JsonNode register = postJson("/api/auth/register", """
                {"username":"smoke-%s","email":"smoke-%s@test.com","password":"password123","firstName":"Smoke","lastName":"Test"}
                """.formatted(random, random), status().isCreated());
        String smokeAccessToken = requireTextField(register, "accessToken");

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        MvcResult meResult = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andReturn();
        if (meResult.getResponse().getStatus() != 200) {
            throw new AssertionError("GET /api/users/me failed with status "
                    + meResult.getResponse().getStatus()
                    + " body: " + meResult.getResponse().getContentAsString());
        }
        putJson("/api/users/me",
                "{\"firstName\":\"Alice\",\"lastName\":\"Dupont\",\"email\":\"alice+" + random + "@test.com\"}",
                accessToken, status().isOk());
        putJson("/api/users/me/password",
                "{\"currentPassword\":\"password123\",\"newPassword\":\"password456\"}",
                smokeAccessToken, status().isNoContent());

        mockMvc.perform(get("/api/places").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        JsonNode createdPlace = postJson("/api/places", """
                {"title":"Smoke Place","description":"desc","latitude":48.85,"longitude":2.35,"tags":["smoke","test"]}
                """, accessToken, status().isCreated());
        JsonNode createdPlaceData = unwrapApiResponseData(createdPlace);
        long placeId = createdPlaceData.path("id").asLong();

        mockMvc.perform(get("/api/places/" + placeId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        putJson("/api/places/" + placeId,
                """
                {"title":"Smoke Place Updated","description":"desc2","latitude":48.851,"longitude":2.351,"tags":["smoke","updated"]}
                """, accessToken, status().isOk());
        mockMvc.perform(get("/api/places/search").param("q", "Smoke").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        MvcResult nearbyResult = mockMvc.perform(get("/api/places/nearby")
                        .param("lat", "48.85")
                        .param("lon", "2.35")
                        .param("radius", "10")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        if (nearbyResult.getResponse().getStatus() != 200) {
            throw new AssertionError("GET /api/places/nearby failed with status "
                    + nearbyResult.getResponse().getStatus()
                    + " body: " + nearbyResult.getResponse().getContentAsString());
        }
        mockMvc.perform(get("/api/places/tags").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/places/" + placeId + "/tags/newtag")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/places/" + placeId + "/tags/newtag")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "smoke.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "fake-image-content".getBytes(StandardCharsets.UTF_8));
        JsonNode withImage = multipartPost("/api/places/" + placeId + "/image", image, accessToken, status().isOk());
        JsonNode withImageData = unwrapApiResponseData(withImage);
        String imageUrl = requireTextField(withImageData, "imageUrl");
        long imageId = Long.parseLong(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
        mockMvc.perform(get("/api/images/" + imageId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/images/" + imageId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/places/" + placeId + "/image").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/collections").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        JsonNode collections = getJson("/api/collections", accessToken, status().isOk());
        JsonNode collectionsData = unwrapApiResponseData(collections);
        long collectionId = findCollectionIdByTag(collectionsData, "updated");
        if (collectionId < 0) {
            throw new AssertionError("No automatic collection found for tag 'updated': " + collectionsData);
        }
        mockMvc.perform(post("/api/collections")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"manual\",\"tagFilter\":\"manual\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/collections/" + collectionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"manual\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/collections/" + collectionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/api/collections/" + collectionId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/collections/" + collectionId + "/places").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/collections/" + collectionId + "/export")
                        .param("format", "geojson")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        JsonNode sharedCollection = postJson("/api/collections/" + collectionId + "/share",
                "{\"label\":\"share-smoke\"}",
                accessToken, status().isOk());
        JsonNode sharedCollectionData = unwrapApiResponseData(sharedCollection);
        String collectionShareToken = requireTextField(sharedCollectionData, "token");

        JsonNode sharedPlace = postJson("/api/places/" + placeId + "/share",
                "{\"label\":\"share-place-smoke\"}",
                accessToken, status().isOk());
        JsonNode sharedPlaceData = unwrapApiResponseData(sharedPlace);
        String placeShareToken = requireTextField(sharedPlaceData, "token");

        JsonNode createdToken = postJson("/api/tokens",
                """
                {"resourceType":"PLACE","resourceId":%d,"permission":"READ","label":"smoke-place-token"}
                """.formatted(placeId),
                accessToken, status().isCreated());
        JsonNode createdTokenData = unwrapApiResponseData(createdToken);
        long tokenId = createdTokenData.path("id").asLong();
        String placeToken = requireTextField(createdTokenData, "token");

        mockMvc.perform(get("/api/tokens").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tokens/" + tokenId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        patchJson("/api/tokens/" + tokenId + "/expiration",
                "{\"expiresAt\":\"" + LocalDateTime.now().plusDays(1) + "\"}",
                accessToken, status().isOk());
        mockMvc.perform(get("/api/tokens/discover").param("token", placeToken))
                .andExpect(status().isOk());

        // Shared-token flows (without JWT)
        mockMvc.perform(get("/api/places/" + placeId).param("token", placeShareToken))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/collections/" + collectionId).param("token", collectionShareToken))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/collections/" + collectionId + "/places").param("token", collectionShareToken))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/api/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":48.8566,\"longitude\":2.3522,\"accuracy\":15}")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/location/current").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        JsonNode sharedLoc = postJson("/api/location/share", "", accessToken, status().isOk(), false);
        JsonNode sharedLocData = unwrapApiResponseData(sharedLoc);
        String shareToken = requireTextField(sharedLocData, "token");
        mockMvc.perform(get("/api/location/public/" + shareToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/location/share").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/location/current").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        String geojson = """
                {"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2.33,48.86]},"properties":{"title":"Imported Smoke"}}]}
                """;
        JsonNode firstImport = postJson("/api/import",
                "{\"content\":" + objectMapper.writeValueAsString(geojson) + ",\"format\":\"GEOJSON\",\"defaultTags\":[\"smoke-import\"],\"skipDuplicates\":true}",
                accessToken, status().isOk());
        JsonNode firstImportData = unwrapApiResponseData(firstImport);
        if (firstImportData.path("imported").asInt(-1) < 1) {
            throw new AssertionError("Expected first import to import at least one place: " + firstImportData);
        }
        if (!hasTagInImportedPlaces(firstImportData.path("places"), "smoke-import")) {
            throw new AssertionError("Expected default tag 'smoke-import' in first import: " + firstImportData);
        }

        MockMultipartFile importFile = new MockMultipartFile(
                "file", "import.geojson", MediaType.APPLICATION_JSON_VALUE, geojson.getBytes(StandardCharsets.UTF_8));
        JsonNode secondImport = multipartPost("/api/import", importFile, accessToken, status().isOk());
        JsonNode secondImportData = unwrapApiResponseData(secondImport);
        if (secondImportData.path("imported").asInt(-1) != 0 || secondImportData.path("skipped").asInt(0) < 1) {
            throw new AssertionError("Expected duplicate import to be skipped: " + secondImportData);
        }

        JsonNode thirdImport = postJson("/api/import",
                "{\"content\":" + objectMapper.writeValueAsString(geojson) + ",\"format\":\"GEOJSON\",\"skipDuplicates\":false}",
                accessToken, status().isOk());
        JsonNode thirdImportData = unwrapApiResponseData(thirdImport);
        if (thirdImportData.path("imported").asInt(-1) < 1) {
            throw new AssertionError("Expected import with skipDuplicates=false to insert again: " + thirdImportData);
        }

        mockMvc.perform(delete("/api/tokens/" + tokenId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/places/" + placeId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    private JsonNode postJson(String url, String body,
                              org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        return postJson(url, body, null, expected);
    }

    private JsonNode postJson(String url, String body, String token,
                              org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        return postJson(url, body, token, expected, true);
    }

    private JsonNode postJson(String url, String body, String token,
                              org.springframework.test.web.servlet.ResultMatcher expected,
                              boolean withJsonContentType) throws Exception {
        var builder = post(url);
        if (withJsonContentType) builder.contentType(MediaType.APPLICATION_JSON);
        if (body != null && !body.isBlank()) builder.content(body);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        MvcResult result = mockMvc.perform(builder).andExpect(expected).andReturn();
        String content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private JsonNode getJson(String url, String token,
                             org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        var builder = get(url);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        MvcResult result = mockMvc.perform(builder).andExpect(expected).andReturn();
        String content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private JsonNode putJson(String url, String body, String token,
                             org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected)
                .andReturn();
        String content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private JsonNode patchJson(String url, String body, String token,
                               org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected)
                .andReturn();
        String content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private JsonNode multipartPost(String url, MockMultipartFile file, String token,
                                   org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(multipart(url)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected)
                .andReturn();
        String content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private String requireTextField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull() || !fieldNode.isTextual()) {
            throw new AssertionError("Expected textual field '" + fieldName + "' in response: " + node);
        }
        return fieldNode.asText();
    }

    private JsonNode unwrapApiResponseData(JsonNode response) {
        JsonNode dataNode = response.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            throw new AssertionError("Expected 'data' field in API response: " + response);
        }
        return dataNode;
    }

    private long findCollectionIdByTag(JsonNode collections, String expectedTag) {
        if (!collections.isArray()) return -1L;
        for (JsonNode c : collections) {
            String tag = c.path("tagFilter").asText(null);
            if (expectedTag.equals(tag)) {
                return c.path("id").asLong(-1L);
            }
        }
        return -1L;
    }

    private boolean hasTagInImportedPlaces(JsonNode places, String expectedTag) {
        if (!places.isArray()) {
            return false;
        }
        for (JsonNode place : places) {
            JsonNode tags = place.path("tags");
            if (!tags.isArray()) {
                continue;
            }
            for (JsonNode tag : tags) {
                if (expectedTag.equals(tag.asText())) {
                    return true;
                }
            }
        }
        return false;
    }
}
