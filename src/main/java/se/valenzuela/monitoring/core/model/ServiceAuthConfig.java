package se.valenzuela.monitoring.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import se.valenzuela.monitoring.core.auth.AuthType;

import java.time.Instant;

/**
 * Stores the auth type and non-sensitive configuration for a monitored service.
 * Sensitive credentials (passwords, tokens, private keys) are stored in Vault,
 * keyed by serviceId. See VaultSecretStore.
 */
@Getter
@Setter
@Entity
@Table(name = "service_auth_config")
public class ServiceAuthConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false, unique = true)
    private MonitoredService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthType authType = AuthType.NONE;

    /**
     * JSON-encoded non-sensitive config fields for the selected auth type.
     * Deserialised to AuthConfigDetails by ServiceAuthProviderFactory.
     * <p>
     * BASIC:  { "username": "..." }
     * OAUTH2: { "tokenUrl": "...", "clientId": "...", "scope": "..." }
     * BEARER_TOKEN / MTLS: null or {}
     */
    @Column(columnDefinition = "TEXT")
    private String configJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected ServiceAuthConfig() {
    }

    public ServiceAuthConfig(MonitoredService service) {
        this.service = service;
    }
}
