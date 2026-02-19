package se.valenzuela.monitoring.notification.channel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.*;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty("spring.mail.host")
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final JsonMapper jsonMapper;

    public EmailNotificationChannel(JavaMailSender mailSender, JsonMapper jsonMapper) {
        this.mailSender = mailSender;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String channelType() {
        return "EMAIL";
    }

    @Override
    public String displayName() {
        return "Email";
    }

    @Override
    public void send(MonitoringEvent event, String configJson) {
        try {
            JsonNode config = jsonMapper.readTree(configJson);
            String recipients = config.path("recipients").asText("");
            String fromAddress = config.path("fromAddress").asText("bootguard@localhost");
            String subjectPrefix = config.path("subjectPrefix").asText("[BootGuard]");

            if (recipients.isBlank()) {
                log.debug("Email notification skipped: no recipients configured");
                return;
            }

            String[] to = recipients.split(",");
            String subject = buildSubject(event, subjectPrefix);
            String body = buildBody(event);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email notification sent for service '{}' to {}", serviceName(event), recipients);
        } catch (Exception e) {
            log.error("Failed to send email notification for service '{}'", serviceName(event), e);
        }
    }

    @Override
    public boolean validate(String configJson) {
        try {
            JsonNode config = jsonMapper.readTree(configJson);
            return config.has("recipients") && config.has("fromAddress");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String configDescription() {
        return "JSON with keys: recipients (comma-separated emails), fromAddress, subjectPrefix";
    }

    @Override
    public List<ConfigField> configFields() {
        return List.of(
                new ConfigField("recipients", "Recipients", true, "", "Email addresses", FieldType.EMAIL_LIST),
                new ConfigField("fromAddress", "From address", true, "bootguard@localhost", "Sender email address"),
                new ConfigField("subjectPrefix", "Subject prefix", false, "[BootGuard]", "Prefix for email subjects")
        );
    }

    private String buildSubject(MonitoringEvent event, String prefix) {
        return switch (event) {
            case ServiceHealthChangedEvent e -> e.wentDown()
                    ? "%s Service DOWN: %s".formatted(prefix, serviceName(e))
                    : "%s Service UP: %s".formatted(prefix, serviceName(e));
            case ServiceAddedEvent e -> "%s Service Added: %s".formatted(prefix, serviceName(e));
            case ServiceRemovedEvent e -> "%s Service Removed: %s".formatted(prefix, serviceName(e));
        };
    }

    private String buildBody(MonitoringEvent event) {
        return switch (event) {
            case ServiceHealthChangedEvent e -> e.wentDown()
                    ? "Service '%s' (%s) is now DOWN.\nDetected at: %s".formatted(
                    serviceName(e), e.service().getUrl(), e.timestamp())
                    : "Service '%s' (%s) is now UP.\nDetected at: %s".formatted(
                    serviceName(e), e.service().getUrl(), e.timestamp());
            case ServiceAddedEvent e ->
                    "Service '%s' (%s) has been added to monitoring.\nAdded at: %s".formatted(
                            serviceName(e), e.service().getUrl(), e.timestamp());
            case ServiceRemovedEvent e ->
                    "Service '%s' (%s) has been removed from monitoring.\nRemoved at: %s".formatted(
                            serviceName(e), e.service().getUrl(), e.timestamp());
        };
    }

    private String serviceName(MonitoringEvent event) {
        MonitoredService service = event.service();
        return service.getName() != null ? service.getName() : service.getUrl();
    }
}
