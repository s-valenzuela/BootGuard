package se.valenzuela.monitoring.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.repository.EnvironmentRepository;
import se.valenzuela.monitoring.core.repository.MonitoredServiceRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private MonitoredServiceRepository serviceRepository;

    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        environmentService = new EnvironmentService(environmentRepository, serviceRepository);
    }

    @Test
    void getAllEnvironments_delegatesToRepository() {
        var prod = new Environment("Production", "#22C55E", 1);
        var qa = new Environment("QA", "#3B82F6", 2);
        when(environmentRepository.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(prod, qa));

        List<Environment> result = environmentService.getAllEnvironments();

        assertEquals(2, result.size());
        assertEquals("Production", result.get(0).getName());
        verify(environmentRepository).findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Test
    void createEnvironment_savesAndReturns() {
        when(environmentRepository.save(any(Environment.class))).thenAnswer(invocation -> {
            Environment env = invocation.getArgument(0);
            env.setId(1L);
            return env;
        });

        Environment result = environmentService.createEnvironment("Production", "#22C55E", 1);

        assertNotNull(result);
        assertEquals("Production", result.getName());
        assertEquals("#22C55E", result.getColor());
        assertEquals(1, result.getDisplayOrder());
        verify(environmentRepository).save(any(Environment.class));
    }

    @Test
    void deleteEnvironment_delegatesToRepository() {
        var env = new Environment("Production", null, 0);

        environmentService.deleteEnvironment(env);

        verify(environmentRepository).delete(env);
    }

    @Test
    void existsByName_delegatesToRepository() {
        when(environmentRepository.existsByName("Production")).thenReturn(true);
        when(environmentRepository.existsByName("Unknown")).thenReturn(false);

        assertTrue(environmentService.existsByName("Production"));
        assertFalse(environmentService.existsByName("Unknown"));
    }

    @Test
    void updateServiceEnvironments_refetchesAndReplacesSet() {
        var service = new MonitoredService("http://localhost:8080");
        service.setId(1L);
        service.setEnvironments(new HashSet<>(Set.of(new Environment("Old", null, 0))));

        when(serviceRepository.findByIdWithEnvironments(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(any(MonitoredService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var newEnv = new Environment("Production", "#22C55E", 1);
        newEnv.setId(10L);

        environmentService.updateServiceEnvironments(service, Set.of(newEnv));

        verify(serviceRepository).findByIdWithEnvironments(1L);
        verify(serviceRepository).save(service);
        assertEquals(1, service.getEnvironments().size());
        assertTrue(service.getEnvironments().contains(newEnv));
    }

    @Test
    void updateServiceEnvironments_serviceNotFound_throws() {
        var service = new MonitoredService("http://localhost:8080");
        service.setId(99L);

        when(serviceRepository.findByIdWithEnvironments(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> environmentService.updateServiceEnvironments(service, Set.of()));
    }

    @Test
    void getEnvironmentsForService_loadsEnvironments() {
        var service = new MonitoredService("http://localhost:8080");
        service.setId(1L);
        var env = new Environment("Production", "#22C55E", 1);
        env.setId(10L);
        service.setEnvironments(new HashSet<>(Set.of(env)));

        when(serviceRepository.findByIdWithEnvironments(1L)).thenReturn(Optional.of(service));

        Set<Environment> result = environmentService.getEnvironmentsForService(1L);

        assertEquals(1, result.size());
        assertTrue(result.contains(env));
        verify(serviceRepository).findByIdWithEnvironments(1L);
    }

    @Test
    void getEnvironmentsForService_serviceNotFound_returnsEmptySet() {
        when(serviceRepository.findByIdWithEnvironments(99L)).thenReturn(Optional.empty());

        Set<Environment> result = environmentService.getEnvironmentsForService(99L);

        assertTrue(result.isEmpty());
    }
}
