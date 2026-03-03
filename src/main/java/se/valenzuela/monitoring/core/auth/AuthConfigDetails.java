package se.valenzuela.monitoring.core.auth;

/**
 * Non-sensitive configuration for a service's auth method.
 * Serialised to JSON and stored in service_auth_config.config_json.
 * Sensitive values (password, token, clientSecret, PEM keys) live in Vault.
 */
public record AuthConfigDetails(
        String username,    // BASIC
        String tokenUrl,    // OAUTH2
        String clientId,    // OAUTH2
        String scope        // OAUTH2
) {
    public static AuthConfigDetails empty() {
        return new AuthConfigDetails(null, null, null, null);
    }
}
