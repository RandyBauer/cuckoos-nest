package io.github.randybauer.cuckoosnest.decoy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.randybauer.cuckoosnest.decoy.types.UrlDecoyType;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecoyTypeRegistryTests {

    @Test
    void findsTypesByIdAndListsThemSorted() {
        DecoyTypeRegistry registry = new DecoyTypeRegistry(List.of(fake("ZEBRA"), new UrlDecoyType(), fake("APPLE")));

        assertThat(registry.get("URL")).isInstanceOf(UrlDecoyType.class);
        assertThat(registry.find("NOPE")).isEmpty();
        assertThat(registry.all()).extracting(DecoyType::id).containsExactly("APPLE", "URL", "ZEBRA");
    }

    @Test
    void unknownTypeErrorNamesTheKnownTypes() {
        DecoyTypeRegistry registry = new DecoyTypeRegistry(List.of(new UrlDecoyType()));

        assertThatThrownBy(() -> registry.get("FAX"))
                .isInstanceOf(UnknownDecoyTypeException.class)
                .hasMessageContaining("FAX")
                .hasMessageContaining("URL");
    }

    @Test
    void twoPluginsWithTheSameIdStopStartup() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new DecoyTypeRegistry(List.of(fake("URL"), new UrlDecoyType())))
                .withMessageContaining("share the id 'URL'");
    }

    @Test
    void badIdsStopStartup() {
        assertThatIllegalStateException().isThrownBy(() -> new DecoyTypeRegistry(List.of(fake("url"))));
        assertThatIllegalStateException().isThrownBy(() -> new DecoyTypeRegistry(List.of(fake("HAS SPACE"))));
        assertThatIllegalStateException().isThrownBy(() -> new DecoyTypeRegistry(List.of(fake(""))));
    }

    private static DecoyType fake(String id) {
        return new DecoyType() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String displayName() {
                return id;
            }

            @Override
            public String newToken(TokenGenerator tokens) {
                return tokens.base62(8);
            }

            @Override
            public String bait(Decoy decoy, String baseUrl) {
                return decoy.getToken();
            }
        };
    }
}
