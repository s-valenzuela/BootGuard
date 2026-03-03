package se.valenzuela.monitoring.core.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reads and writes service credentials in HashiCorp Vault using the KV v2 API.
 *
 * <p>Secrets are stored at:
 * <pre>  {mount}/data/bootguard/services/{serviceId}</pre>
 *
 * <p>The RestClient used here is intentionally separate from the shared monitoring
 * RestClient: it does not carry client certificates and connects to Vault over plain
 * HTTP (or HTTPS, if VAULT_URI points to an HTTPS endpoint).
 *
 * <p>If Vault is not configured ({@link VaultProperties#isConfigured()} returns
 * {@code false}), all reads return {@link Optional#empty()} and writes are silently
 * skipped. This allows BootGuard to start without Vault when no service uses
 * credential-based auth.
 */
@Slf4j
@Service
@EnableConfigurationProperties(VaultProperties.class)
public class VaultSecretStore {

    private final VaultProperties props;
    private final RestClient vaultClient;

    public VaultSecretStore(VaultProperties props) {
        this.props = props;
        if (props.isConfigured()) {
            this.vaultClient = RestClient.builder()
                    .baseUrl(props.uri())
                    .defaultHeader("X-Vault-Token", props.token())
                    .build();
        } else {
            this.vaultClient = null;
            log.info("Vault not configured — service credential storage is disabled");
        }
    }

    /**
     * Reads the secrets for the given service from Vault.
     * Returns {@link Optional#empty()} when Vault is not configured or the path
     * does not yet contain a secret.
     */
    public Optional<ServiceSecrets> readSecrets(Long serviceId) {
        if (vaultClient == null) {
            return Optional.empty();
        }
        try {
            KvReadResponse response = vaultClient.get()
                    .uri(props.dataPath(serviceId))
                    .retrieve()
                    .body(KvReadResponse.class);

            if (response == null || response.data() == null || response.data().data() == null) {
                return Optional.empty();
            }
            Map<String, String> data = response.data().data();
            return Optional.of(new ServiceSecrets(
                    data.get("password"),
                    data.get("token"),
                    data.get("clientSecret"),
                    data.get("certificatePem"),
                    data.get("privateKeyPem")
            ));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("Failed to read secrets from Vault for service {}: {}", serviceId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Writes (or updates) the secrets for the given service in Vault.
     * Null values in {@code secrets} are omitted from the payload — they do not
     * overwrite previously stored values.
     */
    public void writeSecrets(Long serviceId, ServiceSecrets secrets) {
        if (vaultClient == null) {
            log.warn("Vault not configured — secrets for service {} were not stored", serviceId);
            return;
        }
        Map<String, String> data = new HashMap<>();
        putIfPresent(data, "password", secrets.password());
        putIfPresent(data, "token", secrets.token());
        putIfPresent(data, "clientSecret", secrets.clientSecret());
        putIfPresent(data, "certificatePem", secrets.certificatePem());
        putIfPresent(data, "privateKeyPem", secrets.privateKeyPem());

        try {
            vaultClient.post()
                    .uri(props.dataPath(serviceId))
                    .body(new KvWriteRequest(data))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Wrote secrets to Vault for service {}", serviceId);
        } catch (RestClientResponseException e) {
            log.error("Failed to write secrets to Vault for service {}: {}", serviceId, e.getMessage());
            throw new IllegalStateException("Could not persist credentials to Vault", e);
        }
    }

    /**
     * Permanently deletes all secret versions for the given service from Vault.
     * Call this when a service is removed.
     */
    public void deleteSecrets(Long serviceId) {
        if (vaultClient == null) {
            return;
        }
        try {
            vaultClient.delete()
                    .uri(props.metadataPath(serviceId))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Deleted secrets from Vault for service {}", serviceId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.warn("Failed to delete secrets from Vault for service {}: {}", serviceId, e.getMessage());
            }
        }
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    // ── Vault KV v2 response / request types ────────────────────────────────

    private record KvReadResponse(KvData data) {
        private record KvData(Map<String, String> data) {}
    }

    private record KvWriteRequest(Map<String, String> data) {}
}
