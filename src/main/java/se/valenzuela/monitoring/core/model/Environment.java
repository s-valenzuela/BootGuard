package se.valenzuela.monitoring.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import se.valenzuela.monitoring.core.auth.AuthType;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "environment")
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 7)
    private String color;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "health_check_interval_seconds")
    private Integer healthCheckIntervalSeconds;

    /**
     * Default auth type applied to services in this environment that have no
     * service-level auth configured. Resolved using the environment with the
     * lowest {@code displayOrder} when a service belongs to multiple environments.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_auth_type", nullable = false, length = 30)
    private AuthType defaultAuthType = AuthType.NONE;

    /**
     * JSON-encoded non-sensitive config for the default auth type.
     * Same structure as {@code ServiceAuthConfig.configJson}.
     * Sensitive credentials are stored in Vault under
     * {@code secret/bootguard/environments/{id}}.
     */
    @Column(name = "default_auth_config_json", columnDefinition = "TEXT")
    private String defaultAuthConfigJson;

    protected Environment() {
    }

    public Environment(String name, String color, int displayOrder) {
        this.name = name;
        this.color = color;
        this.displayOrder = displayOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Environment that = (Environment) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
