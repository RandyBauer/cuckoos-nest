package io.github.randybauer.cuckoosnest.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** The dashboard login: one admin whose password is configured only as a bcrypt hash. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "nest.admin.username=keeper",
            // bcrypt of "test-password" (test only)
            "nest.admin.password-hash=$2y$10$QUCq3AjzYpVVf6aBHhjihuaWRrF3polNbLe4aWxeJFxvJ0dIQWDg."
        })
@AutoConfigureMockMvc
class LoginTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void theLoginPageIsPublicAndLabelsItsFields() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<label for=\"username\">")))
                .andExpect(content().string(Matchers.containsString("<label for=\"password\">")))
                .andExpect(content().string(Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void theRightPasswordSignsIn() throws Exception {
        mvc.perform(formLogin().user("keeper").password("test-password"))
                .andExpect(authenticated().withUsername("keeper").withRoles("ADMIN"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void aWrongPasswordOrUserDoesNot() throws Exception {
        mvc.perform(formLogin().user("keeper").password("nope"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin().user("admin").password("test-password")).andExpect(unauthenticated());
    }

    @Test
    void aLoginPostWithoutACsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/login").param("username", "keeper").param("password", "test-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signingOutGoesBackToTheLoginPage() throws Exception {
        mvc.perform(logout()).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void theHomePageNeedsASignIn() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
