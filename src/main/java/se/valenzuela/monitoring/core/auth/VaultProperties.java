package se.valenzuela.monitoring.core.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the HashiCorp Vault secret store.
 *
 * <pre>
 * bootguard:
 *   vault:
 *     uri: ${VAULT_URI:http://localhost:8200}
 *     token: ${VAULT_TOKEN:dev-root-token}
 *     secret-mount-path: secret
 * </pre>
 *
 * In production the token should be supplied via the VAULT_TOKEN environment
 * variable, not hard-coded in application.yaml.
 */
@ConfigurationProperties("bootguard.vault")
public record VaultProperties(
        String uri,
        String token,
        String secretMountPath
) {
    public boolean isConfigured() {
        return uri != null && !uri.isBlank() && token != null && !token.isBlank();
    }

    /** Vault KV v2 data path for a given service. */
    String dataPath(Long serviceId) {
        String mount = secretMountPath != null ? secretMountPath : "secret";
        return "/v1/%s/data/bootguard/services/%d".formatted(mount, serviceId);
    }

    /** Vault KV v2 metadata path — used for hard-deletes. */
    String metadataPath(Long serviceId) {
        String mount = secretMountPath != null ? secretMountPath : "secret";
        return "/v1/%s/metadata/bootguard/services/%d".formatted(mount, serviceId);
    }
}
