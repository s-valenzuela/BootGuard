package se.valenzuela.monitoring.notification.channel;

import se.valenzuela.monitoring.notification.event.MonitoringEvent;

public interface NotificationChannel {

    String channelType();

    String displayName();

    void send(MonitoringEvent event, String configJson);

    boolean validate(String configJson);

    String configDescription();
}
