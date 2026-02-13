package se.valenzuela.monitoring.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import se.valenzuela.monitoring.client.InfoEndpointResponse;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "monitored_service")
public class MonitoredService {

    public static final String DEFAULT_INFO_ENDPOINT = "/actuator/info";
    public static final String DEFAULT_HEALTH_ENDPOINT = "/actuator/health";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 2048)
    private String url;

    private String name;
    private String version;
    private Instant lastUpdated;

    @Transient
    private boolean infoStatus;

    @Transient
    private boolean healthStatus;

    @Transient
    private String healthResponseStatus;

    @Column(nullable = false)
    private String infoEndpoint;

    @Column(nullable = false)
    private String healthEndpoint;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "service_environment",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "environment_id"))
    private Set<Environment> environments = new HashSet<>();

    protected MonitoredService() {
    }

    public MonitoredService(String url) {
        this.url = url;
        this.lastUpdated = Instant.now();
        this.infoEndpoint = DEFAULT_INFO_ENDPOINT;
        this.healthEndpoint = DEFAULT_HEALTH_ENDPOINT;
    }

    public void updateInfo(InfoEndpointResponse info) {
        this.name = info.name();
        this.version = info.version();
        this.lastUpdated = Instant.now();
    }
}
