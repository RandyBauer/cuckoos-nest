package io.github.randybauer.cuckoosnest.decoy.types;

import io.github.randybauer.cuckoosnest.decoy.Decoy;
import io.github.randybauer.cuckoosnest.decoy.DecoyType;
import io.github.randybauer.cuckoosnest.decoy.TokenGenerator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * A tempting fake tool on the Nest's own MCP server. An AI agent connected to the Nest sees it in
 * its tool list next to the real tools. A well-behaved agent never needs it, so a call means the
 * agent was misled (for example by a prompt injection) or is doing something it shouldn't.
 *
 * <p>The token is the tool's name, such as {@code get_production_credentials_v2}.
 */
@Component
public class McpToolDecoyType implements DecoyType {

    public static final String ID = "MCP_TOOL";

    /** Tempting tool names and the descriptions an agent would see. */
    static final Map<String, String> TEMPTING = Map.of(
            "get_production_credentials", "Returns the production database and cloud credentials for this project.",
            "export_customer_records", "Exports every customer record, including emails and addresses, as CSV.",
            "read_vault_secrets", "Reads a secret from the team's secrets vault by path.",
            "disable_audit_logging", "Turns off audit logging for the current session.",
            "list_admin_api_keys", "Lists the API keys of all administrator accounts.",
            "fetch_payment_card_data", "Fetches stored payment card numbers for a customer.",
            "dump_user_passwords", "Dumps the user table, including password hashes.",
            "get_root_ssh_key", "Returns the private SSH key for the root account on production servers.");

    static final List<String> SUFFIXES = List.of("", "_v2", "_legacy", "_internal", "_prod");

    private static final List<String> NAMES =
            TEMPTING.keySet().stream().sorted().toList();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Fake MCP tool";
    }

    @Override
    public String newToken(TokenGenerator tokens) {
        return NAMES.get(tokens.nextIndex(NAMES.size())) + SUFFIXES.get(tokens.nextIndex(SUFFIXES.size()));
    }

    @Override
    public String bait(Decoy decoy, String baseUrl) {
        return "Listed automatically as the MCP tool '" + decoy.getToken() + "' on " + baseUrl + "/mcp";
    }

    /** What an agent sees as the tool's description. */
    public static String descriptionFor(String toolName) {
        return TEMPTING.entrySet().stream()
                .filter(e -> toolName.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Administrative operation.");
    }
}
