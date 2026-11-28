package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.randybauer.cuckoosnest.TestcontainersConfiguration;
import io.github.randybauer.cuckoosnest.decoy.types.UrlDecoyType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DecoyServiceTests {

    @Autowired
    private DecoyService service;

    @Test
    void createsAUrlDecoyWithAFreshToken() {
        Decoy decoy = service.create("URL", "  Fake admin page  ", "  docs/ADMIN.md ");

        assertThat(decoy.getType()).isEqualTo(UrlDecoyType.ID);
        assertThat(decoy.getToken()).matches("[0-9A-Za-z]{22}");
        assertThat(decoy.getLabel()).isEqualTo("Fake admin page");
        assertThat(decoy.getLocation()).isEqualTo("docs/ADMIN.md");
        assertThat(service.list()).extracting(Decoy::getId).contains(decoy.getId());
    }

    @Test
    void twoDecoysNeverShareAToken() {
        Decoy first = service.create("URL", "one", null);
        Decoy second = service.create("URL", "two", null);

        assertThat(first.getToken()).isNotEqualTo(second.getToken());
    }

    @Test
    void blankLocationIsStoredAsNothing() {
        assertThat(service.create("URL", "label", "   ").getLocation()).isNull();
    }

    @Test
    void rejectsBadInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create("URL", " ", null));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create("URL", null, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.create("URL", "x".repeat(DecoyService.MAX_LABEL + 1), null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.create("URL", "ok", "x".repeat(DecoyService.MAX_LOCATION + 1)));
        assertThatThrownBy(() -> service.create("FAX", "ok", null)).isInstanceOf(UnknownDecoyTypeException.class);
    }

    @Test
    void retiredDecoysAreNoLongerFound() {
        Decoy decoy = service.create("URL", "bait", null);
        assertThat(service.findActive("URL", decoy.getToken())).isPresent();

        service.retire(decoy.getId());

        assertThat(service.findActive("URL", decoy.getToken())).isEmpty();
    }

    @Test
    void findActiveChecksTheType() {
        Decoy decoy = service.create("URL", "bait", null);

        assertThat(service.findActive("CREDENTIAL", decoy.getToken())).isEmpty();
    }

    @Test
    void retiringAMissingDecoyFails() {
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> service.retire(missing))
                .isInstanceOf(DecoyNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }
}
