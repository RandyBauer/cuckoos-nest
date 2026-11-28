package io.github.randybauer.cuckoosnest.trip;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** One IP can hit the public tripwires only so fast (limit lowered to 5 per minute here). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "nest.tripwire.requests-per-minute=5")
@AutoConfigureMockMvc
class TripwireRateLimitTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void theSixthRequestInAMinuteFromOneIpIsRefused() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/t/anything-" + i).with(from("192.0.2.10"))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/t/anything-6").with(from("192.0.2.10")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
        // The same limit and answer apply on the decoy API, so the limit can't tell traps apart.
        mvc.perform(get("/api/v1/users").with(from("192.0.2.10")))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Too Many Requests"));
    }

    @Test
    void otherIpsAndOtherPagesAreNotAffected() throws Exception {
        for (int i = 0; i < 6; i++) {
            mvc.perform(get("/t/x").with(from("192.0.2.20")));
        }
        mvc.perform(get("/t/x").with(from("192.0.2.21"))).andExpect(status().isNotFound());
        for (int i = 0; i < 10; i++) {
            mvc.perform(get("/actuator/health").with(from("192.0.2.20"))).andExpect(status().isOk());
        }
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
