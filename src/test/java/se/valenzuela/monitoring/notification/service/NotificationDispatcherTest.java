package se.valenzuela.monitoring.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.channel.NotificationChannel;
import se.valenzuela.monitoring.notification.event.MonitoringEventCarrier;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationChannel channel1;

    @Mock
    private NotificationChannel channel2;

    @Mock
    private NotificationConfigService configService;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(List.of(channel1, channel2), configService);
    }

    private MonitoringEventCarrier createCarrier() {
        var service = new MonitoredService("http://localhost:8080");
        service.setId(1L);
        service.setName("test-app");
        var event = new ServiceHealthChangedEvent(service, true, false, Instant.now());
        return new MonitoringEventCarrier(this, event);
    }

    @Test
    void enabledChannel_sendsNotification() {
        when(channel1.channelType()).thenReturn("EMAIL");
        when(configService.isEnabledForService("EMAIL", 1L)).thenReturn(true);
        when(configService.getEffectiveConfigJson("EMAIL", 1L)).thenReturn("{\"recipients\":\"a@b.com\"}");
        when(channel2.channelType()).thenReturn("DISCORD");
        when(configService.isEnabledForService("DISCORD", 1L)).thenReturn(false);

        dispatcher.onMonitoringEvent(createCarrier());

        verify(channel1).send(any(), eq("{\"recipients\":\"a@b.com\"}"));
        verify(channel2, never()).send(any(), any());
    }

    @Test
    void disabledChannel_skips() {
        when(channel1.channelType()).thenReturn("EMAIL");
        when(configService.isEnabledForService("EMAIL", 1L)).thenReturn(false);
        when(channel2.channelType()).thenReturn("DISCORD");
        when(configService.isEnabledForService("DISCORD", 1L)).thenReturn(false);

        dispatcher.onMonitoringEvent(createCarrier());

        verify(channel1, never()).send(any(), any());
        verify(channel2, never()).send(any(), any());
    }

    @Test
    void channelException_doesNotAffectOthers() {
        when(channel1.channelType()).thenReturn("EMAIL");
        when(configService.isEnabledForService("EMAIL", 1L)).thenReturn(true);
        when(configService.getEffectiveConfigJson("EMAIL", 1L)).thenReturn("{}");
        doThrow(new RuntimeException("send failed")).when(channel1).send(any(), any());

        when(channel2.channelType()).thenReturn("DISCORD");
        when(configService.isEnabledForService("DISCORD", 1L)).thenReturn(true);
        when(configService.getEffectiveConfigJson("DISCORD", 1L)).thenReturn("{}");

        dispatcher.onMonitoringEvent(createCarrier());

        verify(channel1).send(any(), any());
        verify(channel2).send(any(), any());
    }
}
