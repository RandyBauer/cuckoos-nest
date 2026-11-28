package io.github.randybauer.cuckoosnest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The MCP endpoint ({@code /mcp}) is for AI agents, which can't fill in a login form. They send a
 * bearer token instead: {@code Authorization: Bearer <NEST_MCP_TOKEN>}.
 *
 * <p>The token is compared in constant time. If no token is configured, nobody gets in.
 */
@Configuration
public class McpSecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(McpSecurityConfig.class);

    @Bean
    @Order(1)
    SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, NestProperties properties) throws Exception {
        String token = properties.mcp().token();
        if (token.isBlank()) {
            LOG.warn("nest.mcp.token is not set: the MCP endpoint will refuse every request.");
        }
        http.securityMatcher("/mcp", "/mcp/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("MCP_CLIENT"))
                .addFilterBefore(new BearerTokenFilter(token), AnonymousAuthenticationFilter.class)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    // Written directly: sendError() would forward to /error, which the login rules redirect.
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"unauthorized\"}");
                }));
        return http.build();
    }

    /** Signs the request in as the MCP client when it carries the configured bearer token. */
    static final class BearerTokenFilter extends OncePerRequestFilter {

        private final byte[] expected;

        BearerTokenFilter(String token) {
            this.expected = token.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (expected.length > 0 && header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                byte[] presented = header.substring(7).strip().getBytes(StandardCharsets.UTF_8);
                if (MessageDigest.isEqual(expected, presented)) {
                    SecurityContextHolder.getContext()
                            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                    "mcp-client", null, List.of(new SimpleGrantedAuthority("ROLE_MCP_CLIENT"))));
                }
            }
            chain.doFilter(request, response);
        }
    }
}
