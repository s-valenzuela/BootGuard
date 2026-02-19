package se.valenzuela.monitoring.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.model.NotificationChannelConfig;
import se.valenzuela.monitoring.notification.model.NotificationServiceOverride;
import se.valenzuela.monitoring.notification.repository.NotificationChannelConfigRepository;
import se.valenzuela.monitoring.notification.repository.NotificationServiceOverrideRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConfigServiceTest {

    @Mock
    private NotificationChannelConfigRepository channelConfigRepository;

    @Mock
    private NotificationServiceOverrideRepository overrideRepository;

    private NotificationConfigService configService;

    private final MonitoredService dummyService = new MonitoredService("http://localhost:8080");

    @BeforeEach
    void setUp() {
        configService = new NotificationConfigService(channelConfigRepository, overrideRepository);
    }

    private NotificationServiceOverride createOverride() {
        return new NotificationServiceOverride(dummyService, "EMAIL");
    }

    @Test
    void overrideEnabled_winsOverGlobalDisabled() {
        var override = createOverride();
        override.setEnabled(true);
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.of(override));

        assertTrue(configService.isEnabledForService("EMAIL", 1L));
    }

    @Test
    void overrideDisabled_winsOverGlobalEnabled() {
        var override = createOverride();
        override.setEnabled(false);
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.of(override));

        assertFalse(configService.isEnabledForService("EMAIL", 1L));
    }

    @Test
    void nullOverride_fallsBackToGlobal() {
        var global = new NotificationChannelConfig("EMAIL", true, "{}");
        when(channelConfigRepository.findByChannelType("EMAIL")).thenReturn(Optional.of(global));

        var override = createOverride();
        override.setEnabled(null); // inherit
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.of(override));

        assertTrue(configService.isEnabledForService("EMAIL", 1L));
    }

    @Test
    void noOverride_fallsBackToGlobal() {
        var global = new NotificationChannelConfig("EMAIL", true, "{}");
        when(channelConfigRepository.findByChannelType("EMAIL")).thenReturn(Optional.of(global));
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.empty());

        assertTrue(configService.isEnabledForService("EMAIL", 1L));
    }

    @Test
    void noGlobalConfig_defaultsToDisabled() {
        when(channelConfigRepository.findByChannelType("EMAIL")).thenReturn(Optional.empty());
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.empty());

        assertFalse(configService.isEnabledForService("EMAIL", 1L));
    }

    @Test
    void configJson_overrideWinsOverGlobal() {
        var override = createOverride();
        override.setConfigJson("{\"override\":true}");
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.of(override));

        assertEquals("{\"override\":true}", configService.getEffectiveConfigJson("EMAIL", 1L));
    }

    @Test
    void configJson_blankOverrideFallsBackToGlobal() {
        var global = new NotificationChannelConfig("EMAIL", true, "{\"global\":true}");
        when(channelConfigRepository.findByChannelType("EMAIL")).thenReturn(Optional.of(global));

        var override = createOverride();
        override.setConfigJson("");
        when(overrideRepository.findByServiceIdAndChannelType(1L, "EMAIL")).thenReturn(Optional.of(override));

        assertEquals("{\"global\":true}", configService.getEffectiveConfigJson("EMAIL", 1L));
    }
}
