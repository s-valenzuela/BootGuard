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

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
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

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(restClient);
        stubRestClient();
    }

    @SuppressWarnings("unchecked")
    private void stubRestClient() {
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private void stubSuccessfulResponses() {
        var info = new InfoEndpointResponse("test-app", "A test app", "1.0.0");
        var health = new HealthEndpointResponse("UP");
        when(responseSpec.body(InfoEndpointResponse.class)).thenReturn(info);
        when(responseSpec.body(HealthEndpointResponse.class)).thenReturn(health);
    }

    @Test
    void addService_successful() {
        stubSuccessfulResponses();

        boolean result = monitoringService.addService("http://localhost:8080");

        assertTrue(result);
        assertEquals(1, monitoringService.getServices().size());

        MonitoredService service = monitoringService.getServices().iterator().next();
        assertEquals("test-app", service.getName());
        assertEquals("1.0.0", service.getVersion());
        assertTrue(service.isHealthStatus());
        assertTrue(service.isInfoStatus());
    }

    @Test
    void addService_duplicate_returnsFalse() {
        stubSuccessfulResponses();

        assertTrue(monitoringService.addService("http://localhost:8080"));
        assertFalse(monitoringService.addService("http://localhost:8080"));
        assertEquals(1, monitoringService.getServices().size());
    }

    @Test
    void addService_endpointDown_marksServiceDown() {
        when(responseSpec.body(InfoEndpointResponse.class)).thenThrow(new RuntimeException("Connection refused"));

        boolean result = monitoringService.addService("http://localhost:9999");

        assertTrue(result);
        MonitoredService service = monitoringService.getServices().iterator().next();
        assertFalse(service.isInfoStatus());
        assertFalse(service.isHealthStatus());
    }

    @Test
    void addService_notifiesListeners() {
        stubSuccessfulResponses();
        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        monitoringService.addListener(notified::set);

        monitoringService.addService("http://localhost:8080");

        assertNotNull(notified.get());
        assertEquals("http://localhost:8080", notified.get().getUrl());
    }

    @Test
    void removeService_removesAndNotifiesListeners() {
        stubSuccessfulResponses();
        monitoringService.addService("http://localhost:8080");

        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        monitoringService.addListener(notified::set);

        MonitoredService service = monitoringService.getServices().iterator().next();
        monitoringService.removeService(service);

        assertTrue(monitoringService.getServices().isEmpty());
        assertNotNull(notified.get());
    }

    @Test
    void removeListener_stopsNotifications() {
        stubSuccessfulResponses();
        AtomicReference<MonitoredService> notified = new AtomicReference<>();
        monitoringService.addListener(notified::set);
        monitoringService.removeListener(notified::set);

        monitoringService.addService("http://localhost:8080");

        assertNull(notified.get());
    }

    @Test
    void getServices_returnsUnmodifiableCollection() {
        assertThrows(UnsupportedOperationException.class, () ->
                monitoringService.getServices().add(new MonitoredService("http://test")));
    }
}
