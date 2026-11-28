package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** The miss cache keeps repeated lookups of non-decoys away from Postgres, using real Redis. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MissCacheTests {

    @MockitoSpyBean
    private DecoyRepository repository;

    @MockitoSpyBean
    private MissCache misses;

    @Autowired
    private DecoyService decoys;

    @Test
    void aRepeatedMissIsAnsweredFromRedis() {
        clearInvocations(repository);

        assertThat(decoys.findActive("URL", "random-scanner-token-1")).isEmpty();
        assertThat(decoys.findActive("URL", "random-scanner-token-1")).isEmpty();
        assertThat(decoys.findActive("URL", "random-scanner-token-1")).isEmpty();

        verify(repository, times(1)).findByToken("random-scanner-token-1");
    }

    @Test
    void realDecoysAlwaysComeFromPostgres() {
        Decoy decoy = decoys.create("URL", "Cache test", null);
        clearInvocations(repository);

        assertThat(decoys.findActive("URL", decoy.getToken())).isPresent();
        assertThat(decoys.findActive("URL", decoy.getToken())).isPresent();

        verify(repository, times(2)).findByToken(decoy.getToken());
    }

    @Test
    void creatingADecoyClearsAnyCachedMissForItsToken() {
        // MCP tool names come from a small list, so a miss for a name can be cached before a decoy
        // with that name exists. Creating the decoy must clear it, or the new decoy would be hidden.
        Decoy decoy = decoys.create("MCP_TOOL", "Tempting tool", null);

        verify(misses).forget("MCP_TOOL", decoy.getToken());
        assertThat(decoys.findActive("MCP_TOOL", decoy.getToken())).isPresent();
    }

    @Test
    void forgetRemovesARememberedMiss() {
        misses.rememberMiss("URL", "forget-me");
        assertThat(misses.isKnownMiss("URL", "forget-me")).isTrue();
        assertThat(misses.isKnownMiss("CREDENTIAL", "forget-me")).isFalse();

        misses.forget("URL", "forget-me");

        assertThat(misses.isKnownMiss("URL", "forget-me")).isFalse();
    }

    @Test
    void retiredDecoysStayHidden() {
        Decoy decoy = decoys.create("URL", "Retire me", null);
        decoys.retire(decoy.getId());

        assertThat(decoys.findActive("URL", decoy.getToken())).isEmpty();
        assertThat(decoys.findActive("URL", decoy.getToken())).isEmpty();
    }
}
