package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class ApiIntegrationTestSupport {

    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected AuthSession registerRandomUser(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String password = "password123";
        JsonNode register = postJson("/api/auth/register", """
                {"email":"%s","password":"%s","confirmPassword":"%s","firstName":"Test","lastName":"User"}
                """.formatted(email, password, password), status().isCreated());
        return new AuthSession(
                requireTextField(register, "accessToken"),
                requireTextField(register, "refreshToken"),
                email,
                password
        );
    }

    protected JsonNode postJson(String url, String body, ResultMatcher expected) throws Exception {
        return postJson(url, body, null, expected);
    }

    protected JsonNode postJson(String url, String body, String token, ResultMatcher expected) throws Exception {
        return postJson(url, body, token, expected, true);
    }

    protected JsonNode postJson(String url, String body, String token, ResultMatcher expected,
                                boolean withJsonContentType) throws Exception {
        var builder = post(url);
        if (withJsonContentType) builder.contentType(MediaType.APPLICATION_JSON);
        if (body != null && !body.isBlank()) builder.content(body);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        MvcResult result = mockMvc.perform(builder).andExpect(expected).andReturn();
        return readJsonOrEmpty(result);
    }

    protected JsonNode getJson(String url, String token, ResultMatcher expected) throws Exception {
        var builder = get(url);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        MvcResult result = mockMvc.perform(builder).andExpect(expected).andReturn();
        return readJsonOrEmpty(result);
    }

    protected JsonNode putJson(String url, String body, String token, ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected)
                .andReturn();
        return readJsonOrEmpty(result);
    }

    protected JsonNode patchJson(String url, String body, String token, ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected)
                .andReturn();
        return readJsonOrEmpty(result);
    }

    protected JsonNode multipartPost(String url, MockMultipartFile file, String token,
                                     ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(multipart(url)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected)
                .andReturn();
        return readJsonOrEmpty(result);
    }

    protected String requireTextField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull() || !fieldNode.isTextual()) {
            throw new AssertionError("Expected textual field '" + fieldName + "' in response: " + node);
        }
        return fieldNode.asText();
    }

    protected JsonNode unwrapApiResponseData(JsonNode response) {
        JsonNode dataNode = response.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            throw new AssertionError("Expected 'data' field in API response: " + response);
        }
        return dataNode;
    }

    protected long findCollectionIdByTag(JsonNode collections, String expectedTag) {
        if (!collections.isArray()) return -1L;
        for (JsonNode c : collections) {
            String tag = c.path("tagFilter").asText(null);
            if (expectedTag.equals(tag)) {
                return c.path("id").asLong(-1L);
            }
        }
        return -1L;
    }

    protected boolean hasTagInImportedPlaces(JsonNode places, String expectedTag) {
        if (!places.isArray()) return false;
        for (JsonNode place : places) {
            JsonNode tags = place.path("tags");
            if (!tags.isArray()) continue;
            for (JsonNode tag : tags) {
                if (expectedTag.equals(tag.asText())) return true;
            }
        }
        return false;
    }

    private JsonNode readJsonOrEmpty(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    protected record AuthSession(String accessToken, String refreshToken, String email, String password) {}
}
