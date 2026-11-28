package io.github.randybauer.cuckoosnest.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertRepository;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UrlTripControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DecoyService decoys;

    @Autowired
    private AlertRepository alerts;

    @Test
    void touchingALiveDecoyRecordsAnAlert() throws Exception {
        Decoy decoy = decoys.create("URL", "Fake admin page", "docs/ADMIN.md");

        mvc.perform(get("/t/" + decoy.getToken() + "?from=readme")
                        .header("User-Agent", "sneaky-agent/1.0")
                        .header("Referer", "https://example.com/docs")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.50");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(content().string(UrlTripController.BODY));

        assertThat(alerts.countByDecoyId(decoy.getId())).isEqualTo(1);
        Alert alert = alerts.findAll().stream()
                .filter(a -> a.getDecoy().getId().equals(decoy.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(alert.getChannel()).isEqualTo(Alert.Channel.HTTP);
        assertThat(alert.getSourceIp()).isEqualTo("203.0.113.50");
        assertThat(alert.getUserAgent()).isEqualTo("sneaky-agent/1.0");
        assertThat(alert.getDetails())
                .containsEntry("method", "GET")
                .containsEntry("path", "/t/" + decoy.getToken())
                .containsEntry("query", "from=readme")
                .containsEntry("referer", "https://example.com/docs");
    }

    @Test
    void postsWithoutACsrfTokenStillTripTheWire() throws Exception {
        Decoy decoy = decoys.create("URL", "Fake webhook", null);

        mvc.perform(post("/t/" + decoy.getToken())).andExpect(status().isNotFound());

        assertThat(alerts.countByDecoyId(decoy.getId())).isEqualTo(1);
    }

    @Test
    void unknownAndRetiredTokensRecordNothing() throws Exception {
        Decoy retired = decoys.create("URL", "Old bait", null);
        decoys.retire(retired.getId());
        long before = alerts.count();

        mvc.perform(get("/t/notARealToken")).andExpect(status().isNotFound());
        mvc.perform(get("/t/" + retired.getToken())).andExpect(status().isNotFound());

        assertThat(alerts.count()).isEqualTo(before);
    }

    @Test
    void liveAndUnknownTokensLookExactlyTheSame() throws Exception {
        Decoy decoy = decoys.create("URL", "bait", null);

        MvcResult live = mvc.perform(get("/t/" + decoy.getToken())).andReturn();
        MvcResult unknown = mvc.perform(get("/t/doesNotExist12345678")).andReturn();

        assertThat(live.getResponse().getStatus())
                .isEqualTo(unknown.getResponse().getStatus());
        assertThat(live.getResponse().getContentAsString())
                .isEqualTo(unknown.getResponse().getContentAsString());
        assertThat(live.getResponse().getContentType())
                .isEqualTo(unknown.getResponse().getContentType());
    }
}
