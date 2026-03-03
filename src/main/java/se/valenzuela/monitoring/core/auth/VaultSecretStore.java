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
 * Reads and writes credentials in HashiCorp Vault using the KV v2 API.
 *
 * <p>Secrets are stored under two namespaces:
 * <pre>
 *   {mount}/data/bootguard/services/{serviceId}
 *   {mount}/data/bootguard/environments/{environmentId}
 * </pre>
 *
 * <p>If Vault is not configured ({@link VaultProperties#isConfigured()} returns
 * {@code false}), all reads return {@link Optional#empty()} and writes are silently
 * skipped. This allows BootGuard to start without Vault when no service or
 * environment uses credential-based auth.
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
            log.info("Vault not configured — credential storage is disabled");
        }
    }

    // ── Service secrets ──────────────────────────────────────────────────────

    /** Reads secrets for the given service. Returns empty when not configured or not found. */
    public Optional<ServiceSecrets> readServiceSecrets(Long serviceId) {
        return read(props.serviceDataPath(serviceId), "service " + serviceId);
    }

    /** Writes (or updates) secrets for the given service. Null fields are omitted. */
    public void writeServiceSecrets(Long serviceId, ServiceSecrets secrets) {
        write(props.serviceDataPath(serviceId), secrets, "service " + serviceId);
    }

    /** Permanently deletes all secret versions for the given service. */
    public void deleteServiceSecrets(Long serviceId) {
        delete(props.serviceMetadataPath(serviceId), "service " + serviceId);
    }

    // ── Environment secrets ──────────────────────────────────────────────────

    /** Reads default-auth secrets for the given environment. Returns empty when not configured or not found. */
    public Optional<ServiceSecrets> readEnvironmentSecrets(Long environmentId) {
        return read(props.environmentDataPath(environmentId), "environment " + environmentId);
    }

    /** Writes (or updates) default-auth secrets for the given environment. Null fields are omitted. */
    public void writeEnvironmentSecrets(Long environmentId, ServiceSecrets secrets) {
        write(props.environmentDataPath(environmentId), secrets, "environment " + environmentId);
    }

    /** Permanently deletes all secret versions for the given environment. */
    public void deleteEnvironmentSecrets(Long environmentId) {
        delete(props.environmentMetadataPath(environmentId), "environment " + environmentId);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private Optional<ServiceSecrets> read(String path, String label) {
        if (vaultClient == null) {
            return Optional.empty();
        }
        try {
            KvReadResponse response = vaultClient.get()
                    .uri(path)
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
            log.warn("Failed to read secrets from Vault for {}: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    private void write(String path, ServiceSecrets secrets, String label) {
        if (vaultClient == null) {
            log.warn("Vault not configured — secrets for {} were not stored", label);
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
                    .uri(path)
                    .body(new KvWriteRequest(data))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Wrote secrets to Vault for {}", label);
        } catch (RestClientResponseException e) {
            log.error("Failed to write secrets to Vault for {}: {}", label, e.getMessage());
            throw new IllegalStateException("Could not persist credentials to Vault", e);
        }
    }

    private void delete(String path, String label) {
        if (vaultClient == null) {
            return;
        }
        try {
            vaultClient.delete()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Deleted secrets from Vault for {}", label);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.warn("Failed to delete secrets from Vault for {}: {}", label, e.getMessage());
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
