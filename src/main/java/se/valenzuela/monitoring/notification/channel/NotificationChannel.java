package se.valenzuela.monitoring.notification.channel;

import se.valenzuela.monitoring.notification.event.MonitoringEvent;

import java.util.List;

public interface NotificationChannel {

    String channelType();

    String displayName();

    void send(MonitoringEvent event, String configJson);

    boolean validate(String configJson);

    String configDescription();

    List<ConfigField> configFields();

    record ConfigField(String key, String label, boolean required, String defaultValue, String description,
                       FieldType type) {
        public ConfigField(String key, String label, boolean required, String defaultValue, String description) {
            this(key, label, required, defaultValue, description, FieldType.TEXT);
        }
    }

    enum FieldType {
        TEXT, EMAIL_LIST
    }
}
