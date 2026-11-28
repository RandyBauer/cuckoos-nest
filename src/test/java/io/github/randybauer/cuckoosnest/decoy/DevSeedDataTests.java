package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** The "dev" profile loads sample decoys; other profiles never do (see PersistenceTests). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("dev")
class DevSeedDataTests {

    @Autowired
    private DecoyRepository decoys;

    @Test
    void devProfileLoadsOneSampleDecoyOfEachType() {
        assertThat(decoys.findAll())
                .extracting(Decoy::getType)
                .containsExactlyInAnyOrder("URL", "CREDENTIAL", "MCP_TOOL");
    }
}
