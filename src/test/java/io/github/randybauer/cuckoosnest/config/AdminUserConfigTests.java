package io.github.randybauer.cuckoosnest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AdminUserConfigTests {

    private final AdminUserConfig config = new AdminUserConfig();

    @Test
    void withoutAHashNobodyCanSignIn() {
        UserDetailsService users = config.userDetailsService(properties("admin", ""));

        assertThatThrownBy(() -> users.loadUserByUsername("admin")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void aBareBcryptHashIsTaggedForTheDelegatingEncoder() {
        assertThat(AdminUserConfig.withEncoderId("$2y$10$abc")).isEqualTo("{bcrypt}$2y$10$abc");
        assertThat(AdminUserConfig.withEncoderId(" {bcrypt}$2a$10$abc ")).isEqualTo("{bcrypt}$2a$10$abc");
    }

    @Test
    void theConfiguredHashMatchesItsPassword() {
        String hash = "$2y$10$QUCq3AjzYpVVf6aBHhjihuaWRrF3polNbLe4aWxeJFxvJ0dIQWDg."; // test-password
        String stored = config.userDetailsService(properties("keeper", hash))
                .loadUserByUsername("keeper")
                .getPassword();

        assertThat(config.passwordEncoder().matches("test-password", stored)).isTrue();
        assertThat(config.passwordEncoder().matches("wrong", stored)).isFalse();
    }

    private static NestProperties properties(String username, String hash) {
        return new NestProperties(
                "http://localhost:8080", new NestProperties.Mcp(""), new NestProperties.Admin(username, hash));
    }
}
