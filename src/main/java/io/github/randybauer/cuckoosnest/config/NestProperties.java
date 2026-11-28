package io.github.randybauer.cuckoosnest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code nest.*}.
 *
 * @param publicBaseUrl the address people and agents use to reach this Nest, without a trailing
 *     slash. Bait (decoy URLs, the decoy API address) is built from it.
 * @param mcp settings for the MCP endpoint
 * @param admin the dashboard's one login
 */
@ConfigurationProperties("nest")
public record NestProperties(
        @DefaultValue("http://localhost:8080") String publicBaseUrl,
        @DefaultValue Mcp mcp,
        @DefaultValue Admin admin) {

    public NestProperties {
        publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    /**
     * @param token the bearer token MCP clients must send. Blank means the MCP endpoint refuses
     *     everyone.
     */
    public record Mcp(@DefaultValue("") String token) {}

    /**
     * @param username the dashboard login name
     * @param passwordHash a bcrypt hash of the password (for example from {@code htpasswd -nbBC 10}).
     *     Blank means nobody can sign in to the dashboard.
     */
    public record Admin(
            @DefaultValue("admin") String username,
            @DefaultValue("") String passwordHash) {}

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
