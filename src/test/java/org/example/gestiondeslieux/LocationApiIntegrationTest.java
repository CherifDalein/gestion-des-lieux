package org.example.gestiondeslieux;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LocationApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void location_endpoints_should_support_update_read_share_and_cleanup() throws Exception {
        AuthSession session = registerRandomUser("location");

        postJson("/api/location/update",
                "{\"latitude\":48.8566,\"longitude\":2.3522,\"accuracy\":15}",
                session.accessToken(), status().isOk());

        mockMvc.perform(get("/api/location/current").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        JsonNode shared = postJson("/api/location/share", "", session.accessToken(), status().isOk(), false);
        String shareToken = requireTextField(unwrapApiResponseData(shared), "token");
        mockMvc.perform(get("/api/location/public/" + shareToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/location/share").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/location/current").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());
    }
}
