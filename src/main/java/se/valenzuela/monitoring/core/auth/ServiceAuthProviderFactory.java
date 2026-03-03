package se.valenzuela.monitoring.core.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.valenzuela.monitoring.config.SslContextFactory;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.model.ServiceAuthConfig;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the correct {@link ServiceAuthProvider} for a monitored service using
 * a two-level fallback:
 *
 * <ol>
 *   <li><b>Service level</b> — if the service has a {@link ServiceAuthConfig} with a
 *       non-{@code NONE} auth type, that takes precedence.</li>
 *   <li><b>Environment level</b> — if the service belongs to one or more environments
 *       that have a default auth type configured, the environment with the lowest
 *       {@code displayOrder} (the primary environment) wins.</li>
 *   <li><b>No auth</b> — {@link NoOpAuthProvider} is used as the final fallback.</li>
 * </ol>
 *
 * <p>Providers are cached to avoid rebuilding SSL contexts and re-fetching OAuth2
 * tokens on every health check poll. Call {@link #evictService} or
 * {@link #evictEnvironment} after saving updated auth config to force a rebuild.
 */
@Slf4j
@Service
public class ServiceAuthProviderFactory {

    private final VaultSecretStore vaultSecretStore;
    private final SslContextFactory sslContextFactory;
    private final ObjectMapper objectMapper;

    /** Cache keyed by service ID — holds providers built from service-level config. */
    private final ConcurrentHashMap<Long, ServiceAuthProvider> serviceCache = new ConcurrentHashMap<>();

    /** Cache keyed by environment ID — holds providers built from environment default auth. */
    private final ConcurrentHashMap<Long, ServiceAuthProvider> environmentCache = new ConcurrentHashMap<>();

    public ServiceAuthProviderFactory(VaultSecretStore vaultSecretStore,
                                      SslContextFactory sslContextFactory,
                                      ObjectMapper objectMapper) {
        this.vaultSecretStore = vaultSecretStore;
        this.sslContextFactory = sslContextFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the effective {@link ServiceAuthProvider} for the given service.
     * Checks service-level config first, then falls back to the primary environment.
     */
    public ServiceAuthProvider forService(MonitoredService service) {
        // 1. Service-level auth
        ServiceAuthConfig authConfig = service.getAuthConfig();
        if (hasAuth(authConfig)) {
            return serviceCache.computeIfAbsent(service.getId(),
                    id -> buildFromServiceConfig(authConfig));
        }

        // 2. Environment default auth — pick the environment with the lowest displayOrder
        //    that has a non-NONE default auth type configured
        Environment primaryEnv = service.getEnvironments().stream()
                .filter(e -> e.getDefaultAuthType() != null && e.getDefaultAuthType() != AuthType.NONE)
                .min(Comparator.comparingInt(Environment::getDisplayOrder))
                .orElse(null);

        if (primaryEnv != null) {
            return environmentCache.computeIfAbsent(primaryEnv.getId(),
                    id -> buildFromEnvironment(primaryEnv));
        }

        // 3. No auth
        return NoOpAuthProvider.INSTANCE;
    }

    /**
     * Evicts the cached service-level provider for the given service.
     * Call after saving a {@link ServiceAuthConfig}.
     */
    public void evictService(Long serviceId) {
        serviceCache.remove(serviceId);
        log.debug("Evicted service auth provider cache for service {}", serviceId);
    }

    /**
     * Evicts the cached environment-level provider for the given environment.
     * Call after updating an environment's default auth config.
     */
    public void evictEnvironment(Long environmentId) {
        environmentCache.remove(environmentId);
        log.debug("Evicted environment auth provider cache for environment {}", environmentId);
    }

    // ── Builder helpers ──────────────────────────────────────────────────────

    private ServiceAuthProvider buildFromServiceConfig(ServiceAuthConfig config) {
        AuthConfigDetails details = parseDetails(config.getConfigJson());
        ServiceSecrets secrets = vaultSecretStore.readServiceSecrets(config.getService().getId())
                .orElse(ServiceSecrets.empty());
        return buildProvider(config.getAuthType(), details, secrets);
    }

    private ServiceAuthProvider buildFromEnvironment(Environment env) {
        AuthConfigDetails details = parseDetails(env.getDefaultAuthConfigJson());
        ServiceSecrets secrets = vaultSecretStore.readEnvironmentSecrets(env.getId())
                .orElse(ServiceSecrets.empty());
        return buildProvider(env.getDefaultAuthType(), details, secrets);
    }

    private ServiceAuthProvider buildProvider(AuthType type, AuthConfigDetails details,
                                              ServiceSecrets secrets) {
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

    private boolean hasAuth(ServiceAuthConfig config) {
        return config != null
                && config.getAuthType() != null
                && config.getAuthType() != AuthType.NONE;
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
