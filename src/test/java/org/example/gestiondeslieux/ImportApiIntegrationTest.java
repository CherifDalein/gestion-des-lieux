package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ImportApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void import_endpoints_should_support_json_and_multipart_with_duplicate_handling() throws Exception {
        AuthSession session = registerRandomUser("import");
        String title = "Imported Smoke " + UUID.randomUUID().toString().substring(0, 8);
        String geojson = """
                {"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2.33,48.86]},"properties":{"title":"%s"}}]}
                """.formatted(title);

        JsonNode firstImport = postJson("/api/import",
                "{\"content\":" + objectMapper.writeValueAsString(geojson)
                        + ",\"format\":\"GEOJSON\",\"defaultTags\":[\"smoke-import\"],\"skipDuplicates\":true}",
                session.accessToken(), status().isOk());
        JsonNode firstData = unwrapApiResponseData(firstImport);
        assertTrue(firstData.path("imported").asInt(-1) >= 1);
        assertTrue(hasTagInImportedPlaces(firstData.path("places"), "smoke-import"));

        MockMultipartFile importFile = new MockMultipartFile(
                "file", "import.geojson", MediaType.APPLICATION_JSON_VALUE, geojson.getBytes(StandardCharsets.UTF_8));
        JsonNode secondImport = multipartPost("/api/import", importFile, session.accessToken(), status().isOk());
        JsonNode secondData = unwrapApiResponseData(secondImport);
        assertTrue(secondData.path("imported").asInt(-1) == 0);
        assertTrue(secondData.path("skipped").asInt(0) >= 1);

        JsonNode thirdImport = postJson("/api/import",
                "{\"content\":" + objectMapper.writeValueAsString(geojson)
                        + ",\"format\":\"GEOJSON\",\"skipDuplicates\":false}",
                session.accessToken(), status().isOk());
        JsonNode thirdData = unwrapApiResponseData(thirdImport);
        assertTrue(thirdData.path("imported").asInt(-1) >= 1);
    }
}
