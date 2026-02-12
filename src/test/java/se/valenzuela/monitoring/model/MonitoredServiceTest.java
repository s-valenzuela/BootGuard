package se.valenzuela.monitoring.model;

import org.junit.jupiter.api.Test;
import se.valenzuela.monitoring.client.InfoEndpointResponse;

import static org.junit.jupiter.api.Assertions.*;

class MonitoredServiceTest {

    @Test
    void constructor_setsDefaults() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        assertEquals("http://localhost:8080", service.getUrl());
        assertNull(service.getName());
        assertNull(service.getVersion());
        assertNotNull(service.getLastUpdated());
        assertEquals(MonitoredService.DEFAULT_INFO_ENDPOINT, service.getInfoEndpoint());
        assertEquals(MonitoredService.DEFAULT_HEALTH_ENDPOINT, service.getHealthEndpoint());
    }

    @Test
    void noArgConstructor_existsForJpa() {
        assertDoesNotThrow(() -> {
            var constructor = MonitoredService.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MonitoredService service = constructor.newInstance();
            assertNull(service.getUrl());
        });
    }

    @Test
    void updateInfo_setsFields() {
        MonitoredService service = new MonitoredService("http://localhost:8080");
        var info = new InfoEndpointResponse("my-app", "My App", "2.0.0");

        service.updateInfo(info);

        assertEquals("my-app", service.getName());
        assertEquals("2.0.0", service.getVersion());
    }
}
