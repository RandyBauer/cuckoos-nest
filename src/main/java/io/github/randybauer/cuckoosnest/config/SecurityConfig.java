package io.github.randybauer.cuckoosnest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security rules.
 *
 * <p>Two things are public on purpose:
 *
 * <ul>
 *   <li>the health check, so load balancers and uptime monitors can reach it;
 *   <li>the decoy tripwires under {@code /t/}, because strangers touching them is the whole point.
 *       Scripts often send POSTs without a CSRF token, so CSRF checks are off for that path only.
 * </ul>
 *
 * <p>Everything else requires a login.
 */
@Configuration
public class SecurityConfig {

    static final String TRIPWIRES = "/t/**";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**", TRIPWIRES)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(TRIPWIRES))
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
