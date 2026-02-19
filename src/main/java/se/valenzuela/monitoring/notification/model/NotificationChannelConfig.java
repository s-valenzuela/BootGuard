package se.valenzuela.monitoring.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_channel_config")
public class NotificationChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_type", nullable = false, unique = true, length = 50)
    private String channelType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationChannelConfig() {
    }

    public NotificationChannelConfig(String channelType, boolean enabled, String configJson) {
        this.channelType = channelType;
        this.enabled = enabled;
        this.configJson = configJson;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
