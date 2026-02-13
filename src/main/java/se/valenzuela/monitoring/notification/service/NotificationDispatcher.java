package se.valenzuela.monitoring.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import se.valenzuela.monitoring.notification.channel.NotificationChannel;
import se.valenzuela.monitoring.notification.event.MonitoringEvent;
import se.valenzuela.monitoring.notification.event.MonitoringEventCarrier;

import java.util.List;

@Slf4j
@Service
public class NotificationDispatcher {

    private final List<NotificationChannel> channels;
    private final NotificationConfigService configService;

    public NotificationDispatcher(List<NotificationChannel> channels,
                                  NotificationConfigService configService) {
        this.channels = channels;
        this.configService = configService;
    }

    @Async
    @EventListener
    public void onMonitoringEvent(MonitoringEventCarrier carrier) {
        MonitoringEvent event = carrier.getMonitoringEvent();
        Long serviceId = event.service().getId();

        for (NotificationChannel channel : channels) {
            try {
                if (!configService.isEnabledForService(channel.channelType(), serviceId)) {
                    log.debug("Channel '{}' disabled for service id={}, skipping", channel.channelType(), serviceId);
                    continue;
                }

                String configJson = configService.getEffectiveConfigJson(channel.channelType(), serviceId);
                channel.send(event, configJson);
            } catch (Exception e) {
                log.error("Error dispatching to channel '{}' for service id={}",
                        channel.channelType(), serviceId, e);
            }
        }
    }
}
