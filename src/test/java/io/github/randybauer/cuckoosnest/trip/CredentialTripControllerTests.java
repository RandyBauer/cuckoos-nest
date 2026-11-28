package io.github.randybauer.cuckoosnest.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertRepository;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import io.github.randybauer.cuckoosnest.decoy.TokenGenerator;
import io.github.randybauer.cuckoosnest.decoy.types.CredentialDecoyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CredentialTripControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DecoyService decoys;

    @Autowired
    private AlertRepository alerts;

    @Test
    void usingTheKeyAsABearerTokenRecordsAnAlert() throws Exception {
        Decoy decoy = decoys.create("CREDENTIAL", "Fake payments key", ".env.production");

        mvc.perform(get("/api/v1/payments/refunds")
                        .header("Authorization", "Bearer " + decoy.getToken())
                        .header("User-Agent", "python-requests/2.32"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().json(CredentialTripController.BODY));

        Alert alert = onlyAlertFor(decoy);
        assertThat(alert.getUserAgent()).isEqualTo("python-requests/2.32");
        assertThat(alert.getDetails())
                .containsEntry("method", "GET")
                .containsEntry("path", "/api/v1/payments/refunds")
                .containsEntry("presentedVia", "authorization-header");
    }

    @Test
    void theKeyIsCaughtInTheApiKeyHeaderAndTheQueryString() throws Exception {
        Decoy viaHeader = decoys.create("CREDENTIAL", "Header key", null);
        Decoy viaQuery = decoys.create("CREDENTIAL", "Query key", null);

        mvc.perform(post("/api/v1/orders").header("X-API-Key", viaHeader.getToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users").param("api_key", viaQuery.getToken())).andExpect(status().isUnauthorized());

        assertThat(onlyAlertFor(viaHeader).getDetails()).containsEntry("presentedVia", "x-api-key-header");
        assertThat(onlyAlertFor(viaQuery).getDetails()).containsEntry("presentedVia", "query-parameter");
    }

    @Test
    void wrongUnknownOrRetiredKeysRecordNothing() throws Exception {
        Decoy retired = decoys.create("CREDENTIAL", "Old key", null);
        decoys.retire(retired.getId());
        Decoy urlDecoy = decoys.create("URL", "Not a key", null);
        String unknownButWellFormed = new CredentialDecoyType().newToken(new TokenGenerator());
        long before = alerts.count();

        for (String key :
                new String[] {retired.getToken(), urlDecoy.getToken(), unknownButWellFormed, "nest_garbage", "hunter2"
                }) {
            mvc.perform(get("/api/v1/anything").header("Authorization", "Bearer " + key))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/anything")).andExpect(status().isUnauthorized());

        assertThat(alerts.count()).isEqualTo(before);
    }

    @Test
    void trappedAndWrongKeysGetTheSameAnswer() throws Exception {
        Decoy decoy = decoys.create("CREDENTIAL", "bait", null);

        MvcResult trapped = mvc.perform(bearer(decoy.getToken())).andReturn();
        MvcResult wrong = mvc.perform(bearer("not-a-key")).andReturn();

        assertThat(trapped.getResponse().getStatus())
                .isEqualTo(wrong.getResponse().getStatus());
        assertThat(trapped.getResponse().getContentAsString())
                .isEqualTo(wrong.getResponse().getContentAsString());
        assertThat(trapped.getResponse().getHeader("WWW-Authenticate"))
                .isEqualTo(wrong.getResponse().getHeader("WWW-Authenticate"));
    }

    private static MockHttpServletRequestBuilder bearer(String key) {
        return get("/api/v1/me").header("Authorization", "Bearer " + key);
    }

    private Alert onlyAlertFor(Decoy decoy) {
        assertThat(alerts.countByDecoyId(decoy.getId())).isEqualTo(1);
        return alerts.findAll().stream()
                .filter(a -> a.getDecoy().getId().equals(decoy.getId()))
                .findFirst()
                .orElseThrow();
    }
}
