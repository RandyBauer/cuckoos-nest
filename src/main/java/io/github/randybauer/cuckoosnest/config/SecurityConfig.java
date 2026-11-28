package io.github.randybauer.cuckoosnest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security rules.
 *
 * <p>These are public on purpose:
 *
 * <ul>
 *   <li>the health check, so load balancers and uptime monitors can reach it;
 *   <li>the decoy tripwires: URL decoys under {@code /t/} and the decoy API under {@code /api/v1/}
 *       that fake keys are used against. Strangers touching them is the whole point. Scripts send
 *       requests without CSRF tokens, so CSRF checks are off for those paths only.
 * </ul>
 *
 * <p>Everything else requires a login. The MCP endpoint has its own rules: see {@link McpSecurityConfig}.
 */
@Configuration
public class SecurityConfig {

    static final String[] TRIPWIRES = {"/t/**", "/api/v1/**"};

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers(TRIPWIRES)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(TRIPWIRES))
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
