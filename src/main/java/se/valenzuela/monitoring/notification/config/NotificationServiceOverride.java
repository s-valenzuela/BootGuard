package se.valenzuela.monitoring.notification.config;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import se.valenzuela.monitoring.model.MonitoredService;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_service_override",
        uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "channel_type"}))
public class NotificationServiceOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private MonitoredService service;

    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;

    private Boolean enabled;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationServiceOverride() {
    }

    public NotificationServiceOverride(MonitoredService service, String channelType) {
        this.service = service;
        this.channelType = channelType;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
