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

    /** Vault KV v2 data path for a service. */
    String serviceDataPath(Long serviceId) {
        return dataPath("services", serviceId);
    }

    /** Vault KV v2 data path for an environment. */
    String environmentDataPath(Long environmentId) {
        return dataPath("environments", environmentId);
    }

    /** Vault KV v2 metadata path for a service — used for hard-deletes. */
    String serviceMetadataPath(Long serviceId) {
        return metadataPath("services", serviceId);
    }

    /** Vault KV v2 metadata path for an environment — used for hard-deletes. */
    String environmentMetadataPath(Long environmentId) {
        return metadataPath("environments", environmentId);
    }

    private String dataPath(String entityType, Long id) {
        return "/v1/%s/data/bootguard/%s/%d".formatted(mount(), entityType, id);
    }

    private String metadataPath(String entityType, Long id) {
        return "/v1/%s/metadata/bootguard/%s/%d".formatted(mount(), entityType, id);
    }

    private String mount() {
        return secretMountPath != null ? secretMountPath : "secret";
    }
}
