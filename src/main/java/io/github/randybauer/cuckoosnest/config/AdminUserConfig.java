package io.github.randybauer.cuckoosnest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * The dashboard has one login. Its password is never stored in the repository: only a bcrypt hash,
 * supplied through the {@code NEST_ADMIN_PASSWORD_HASH} environment variable.
 */
@Configuration
public class AdminUserConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserConfig.class);

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(NestProperties properties) {
        NestProperties.Admin admin = properties.admin();
        if (admin.passwordHash().isBlank()) {
            LOG.warn("nest.admin.password-hash is not set: nobody can sign in to the dashboard.");
            return new InMemoryUserDetailsManager();
        }
        return new InMemoryUserDetailsManager(User.withUsername(admin.username())
                .password(withEncoderId(admin.passwordHash()))
                .roles("ADMIN")
                .build());
    }

    /** Accepts a bare bcrypt hash ("$2a$..."/"$2y$...") or one already tagged "{bcrypt}". */
    static String withEncoderId(String hash) {
        String trimmed = hash.strip();
        return trimmed.startsWith("{") ? trimmed : "{bcrypt}" + trimmed;
    }
}
