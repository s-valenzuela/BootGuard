package se.valenzuela.monitoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import se.valenzuela.monitoring.client.HealthEndpointResponse;
import se.valenzuela.monitoring.client.InfoEndpointResponse;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.repository.MonitoredServiceRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private MonitoredServiceRepository repository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(restClient, repository);
    }

    @SuppressWarnings("unchecked")
    private void stubRestClient() {
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void addService_successful() {
        stubRestClient();
        var info = new InfoEndpointResponse("test-app", "A test app", "1.0.0");
        when(responseSpec.body(InfoEndpointResponse.class)).thenReturn(info);
        when(repository.existsByUrl("http://localhost:8080")).thenReturn(false);
        when(repository.save(any(MonitoredService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = monitoringService.addService("http://localhost:8080");

        assertTrue(result);
        verify(repository).save(argThat(service ->
                "test-app".equals(service.getName()) && "1.0.0".equals(service.getVersion())));
    }

    @Test
    void addService_duplicate_returnsFalse() {
        when(repository.existsByUrl("http://localhost:8080")).thenReturn(true);

        assertFalse(monitoringService.addService("http://localhost:8080"));
        verify(repository, never()).save(any());
    }

    @Test
    void addService_endpointDown_stillSaves() {
        stubRestClient();
        when(repository.existsByUrl("http://localhost:9999")).thenReturn(false);
        when(responseSpec.body(InfoEndpointResponse.class)).thenThrow(new RuntimeException("Connection refused"));
        when(repository.save(any(MonitoredService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = monitoringService.addService("http://localhost:9999");

        assertTrue(result);
        verify(repository).save(any(MonitoredService.class));
    }

    @Test
    void addService_notifiesListeners() {
        stubRestClient();
        var info = new InfoEndpointResponse("test-app", "A test app", "1.0.0");
        when(responseSpec.body(InfoEndpointResponse.class)).thenReturn(info);
        when(repository.existsByUrl("http://localhost:8080")).thenReturn(false);
        when(repository.save(any(MonitoredService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        monitoringService.addListener(notified::set);

        monitoringService.addService("http://localhost:8080");

        assertNotNull(notified.get());
        assertEquals("http://localhost:8080", notified.get().getUrl());
    }

    @Test
    void removeService_deletesAndNotifiesListeners() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        monitoringService.addListener(notified::set);

        monitoringService.removeService(service);

        verify(repository).delete(service);
        assertNotNull(notified.get());
    }

    @Test
    void removeListener_stopsNotifications() {
        stubRestClient();
        var info = new InfoEndpointResponse("test-app", "A test app", "1.0.0");
        when(responseSpec.body(InfoEndpointResponse.class)).thenReturn(info);
        when(repository.existsByUrl("http://localhost:8080")).thenReturn(false);
        when(repository.save(any(MonitoredService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        Consumer<MonitoredService> listener = notified::set;
        monitoringService.addListener(listener);
        monitoringService.removeListener(listener);

        monitoringService.addService("http://localhost:8080");

        assertNull(notified.get());
    }

    @Test
    void getServices_fetchesLiveStatus() {
        stubRestClient();
        var service = new MonitoredService("http://localhost:8080");
        when(repository.findAll()).thenReturn(List.of(service));

        var info = new InfoEndpointResponse("test-app", "A test app", "1.0.0");
        var health = new HealthEndpointResponse("UP");
        when(responseSpec.body(InfoEndpointResponse.class)).thenReturn(info);
        when(responseSpec.body(HealthEndpointResponse.class)).thenReturn(health);

        List<MonitoredService> services = monitoringService.getServices();

        assertEquals(1, services.size());
        MonitoredService result = services.getFirst();
        assertTrue(result.isInfoStatus());
        assertTrue(result.isHealthStatus());
        assertEquals("UP", result.getHealthResponseStatus());
        assertEquals("test-app", result.getName());
        assertEquals("1.0.0", result.getVersion());
    }

    @Test
    void getServices_serviceDown_setsStatusFalse() {
        stubRestClient();
        var service = new MonitoredService("http://localhost:8080");
        when(repository.findAll()).thenReturn(List.of(service));
        when(responseSpec.body(InfoEndpointResponse.class)).thenThrow(new RuntimeException("Connection refused"));
        when(responseSpec.body(HealthEndpointResponse.class)).thenThrow(new RuntimeException("Connection refused"));

        List<MonitoredService> services = monitoringService.getServices();

        MonitoredService result = services.getFirst();
        assertFalse(result.isInfoStatus());
        assertFalse(result.isHealthStatus());
        assertEquals("DOWN", result.getHealthResponseStatus());
    }

    @Test
    void updateServiceUrl_savesAndNotifies() {
        MonitoredService service = new MonitoredService("http://localhost:8080");
        when(repository.save(any(MonitoredService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        monitoringService.addListener(notified::set);

        monitoringService.updateServiceUrl(service, "http://localhost:9090");

        assertEquals("http://localhost:9090", service.getUrl());
        verify(repository).save(service);
        assertNotNull(notified.get());
    }
}
