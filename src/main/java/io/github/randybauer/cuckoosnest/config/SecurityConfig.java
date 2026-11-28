package io.github.randybauer.cuckoosnest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security rules.
 *
 * <p>The health check is public so load balancers and uptime monitors can reach it.
 * Everything else requires a login until later features open specific paths on purpose
 * (for example, the public decoy alarm endpoints).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
