package io.github.randybauer.cuckoosnest.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.alert.Alert;
import io.github.randybauer.cuckoosnest.alert.AlertService;
import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyService;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "nest.public-base-url=https://nest.test")
@AutoConfigureMockMvc
@Transactional
class DashboardTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DecoyService decoys;

    @Autowired
    private AlertService alerts;

    @Test
    void theDashboardListsDecoysWithTheirAlertCountsAndRecentAlerts() throws Exception {
        Decoy decoy = decoys.create("URL", "Fake wiki page", "docs/WIKI.md");
        alerts.record(decoy, Alert.Channel.HTTP, "203.0.113.9", "curl/8.9", Map.of());

        String html = page("/");

        assertThat(html)
                .contains("Fake wiki page")
                .contains("docs/WIKI.md")
                .contains("Link (URL)")
                .contains("https://nest.test/t/" + decoy.getToken())
                .contains("203.0.113.9")
                .contains("curl/8.9")
                .contains("Plant a decoy")
                .contains("<html lang=\"en\"");
    }

    @Test
    void creatingADecoyShowsTheBaitToPlant() throws Exception {
        mvc.perform(post("/decoys")
                        .with(user("admin"))
                        .with(csrf())
                        .param("type", "CREDENTIAL")
                        .param("label", "Fake payments key")
                        .param("location", ".env.production"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("createdLabel", "Fake payments key"))
                .andExpect(flash().attribute(
                                "createdBait",
                                Matchers.startsWith("API_BASE_URL=https://nest.test/api/v1\nAPI_KEY=nest_")));

        assertThat(decoys.list()).extracting(Decoy::getLabel).contains("Fake payments key");
    }

    @Test
    void badInputGivesAFriendlyMessageInsteadOfAnError() throws Exception {
        mvc.perform(post("/decoys")
                        .with(user("admin"))
                        .with(csrf())
                        .param("type", "URL")
                        .param("label", " "))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorKey", "error.label.required"));
        mvc.perform(post("/decoys")
                        .with(user("admin"))
                        .with(csrf())
                        .param("type", "FAX")
                        .param("label", "x"))
                .andExpect(flash().attribute("errorKey", "error.type.unknown"));
        mvc.perform(post("/decoys")
                        .with(user("admin"))
                        .with(csrf())
                        .param("type", "URL")
                        .param("label", "x".repeat(201)))
                .andExpect(flash().attribute("errorKey", "error.field.tooLong"));
    }

    @Test
    void retiringFromTheDashboardWorksAndNeedsCsrf() throws Exception {
        Decoy decoy = decoys.create("URL", "Old bait", null);

        mvc.perform(post("/decoys/" + decoy.getId() + "/retire").with(user("admin")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/decoys/" + decoy.getId() + "/retire")
                        .with(user("admin"))
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("retired", true));

        assertThat(decoys.findActive("URL", decoy.getToken())).isEmpty();
    }

    @Test
    void spanishIsOneClickAwayAndRemembered() throws Exception {
        MvcResult result = mvc.perform(get("/").param("lang", "es").with(user("admin")))
                .andExpect(status().isOk())
                .andReturn();
        Cookie remembered = result.getResponse().getCookie("nest-lang");

        assertThat(result.getResponse().getContentAsString())
                .contains("<html lang=\"es\"")
                .contains("Colocar un señuelo")
                .contains("Cerrar sesión");
        assertThat(remembered).isNotNull();
        String next = mvc.perform(get("/").with(user("admin")).cookie(remembered))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(next).contains("Señuelos");
    }

    @Test
    void unsupportedLanguagesAreIgnored() throws Exception {
        assertThat(page("/?lang=xx")).contains("<html lang=\"en\"").contains("Plant a decoy");
    }

    @Test
    void theAlertFragmentRefreshesAndNeedsASignIn() throws Exception {
        mvc.perform(get("/alerts/fragment")).andExpect(status().is3xxRedirection());
        assertThat(page("/alerts/fragment")).contains("id=\"alerts-table\"").doesNotContain("<html");
    }

    @Test
    void theLoginPageIsTranslatedToo() throws Exception {
        String html = mvc.perform(get("/login").param("lang", "es"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(html).contains("Iniciar sesión").contains("Contraseña");
    }

    private String page(String path) throws Exception {
        return mvc.perform(get(path).with(user("admin")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
