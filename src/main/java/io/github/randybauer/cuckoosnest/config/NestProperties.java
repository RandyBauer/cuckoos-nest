package io.github.randybauer.cuckoosnest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code nest.*}.
 *
 * @param publicBaseUrl the address people and agents use to reach this Nest, without a trailing
 *     slash. Bait (decoy URLs, the decoy API address) is built from it.
 * @param mcp settings for the MCP endpoint
 */
@ConfigurationProperties("nest")
public record NestProperties(
        @DefaultValue("http://localhost:8080") String publicBaseUrl,
        @DefaultValue Mcp mcp) {

    public NestProperties {
        publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    /**
     * @param token the bearer token MCP clients must send. Blank means the MCP endpoint refuses
     *     everyone.
     */
    public record Mcp(@DefaultValue("") String token) {}

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
