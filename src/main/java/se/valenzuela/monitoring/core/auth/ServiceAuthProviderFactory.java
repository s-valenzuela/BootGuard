package se.valenzuela.monitoring.core.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.valenzuela.monitoring.config.SslContextFactory;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.model.ServiceAuthConfig;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the correct {@link ServiceAuthProvider} for a monitored service.
 *
 * <p>Providers are cached by service ID to avoid rebuilding SSL contexts and
 * re-fetching OAuth2 tokens on every health check poll. Call {@link #evict}
 * after saving a new {@link ServiceAuthConfig} to force the provider to be
 * rebuilt with the updated configuration on the next poll cycle.
 */
@Slf4j
@Service
public class ServiceAuthProviderFactory {

    private final VaultSecretStore vaultSecretStore;
    private final SslContextFactory sslContextFactory;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Long, ServiceAuthProvider> cache = new ConcurrentHashMap<>();

    public ServiceAuthProviderFactory(VaultSecretStore vaultSecretStore,
                                      SslContextFactory sslContextFactory,
                                      ObjectMapper objectMapper) {
        this.vaultSecretStore = vaultSecretStore;
        this.sslContextFactory = sslContextFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the {@link ServiceAuthProvider} for the given service, building
     * and caching it on first access.
     */
    public ServiceAuthProvider forService(MonitoredService service) {
        ServiceAuthConfig authConfig = service.getAuthConfig();
        if (authConfig == null || authConfig.getAuthType() == null
                || authConfig.getAuthType() == AuthType.NONE) {
            return NoOpAuthProvider.INSTANCE;
        }
        return cache.computeIfAbsent(service.getId(), id -> build(authConfig));
    }

    /**
     * Removes the cached provider for the given service. The next call to
     * {@link #forService} will rebuild it from the current config and Vault secrets.
     * Call this whenever a service's auth config is saved or updated.
     */
    public void evict(Long serviceId) {
        cache.remove(serviceId);
        log.debug("Evicted auth provider cache for service {}", serviceId);
    }

    private ServiceAuthProvider build(ServiceAuthConfig config) {
        AuthType type = config.getAuthType();
        AuthConfigDetails details = parseDetails(config.getConfigJson());
        ServiceSecrets secrets = vaultSecretStore.readSecrets(config.getService().getId())
                .orElse(ServiceSecrets.empty());

        return switch (type) {
            case NONE -> NoOpAuthProvider.INSTANCE;
            case BASIC -> new BasicAuthProvider(details.username(), secrets.password());
            case BEARER_TOKEN -> new BearerTokenProvider(secrets.token());
            case OAUTH2 -> new OAuth2ClientCredentialsProvider(
                    details.tokenUrl(), details.clientId(), secrets.clientSecret(), details.scope());
            case MTLS -> new MtlsAuthProvider(
                    secrets.certificatePem(), secrets.privateKeyPem(), sslContextFactory);
        };
    }

    private AuthConfigDetails parseDetails(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return AuthConfigDetails.empty();
        }
        try {
            return objectMapper.readValue(configJson, AuthConfigDetails.class);
        } catch (Exception e) {
            log.warn("Failed to parse auth config details JSON: {}", e.getMessage());
            return AuthConfigDetails.empty();
        }
    }
}
