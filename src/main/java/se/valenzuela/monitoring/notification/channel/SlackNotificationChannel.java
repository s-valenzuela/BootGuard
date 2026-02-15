package se.valenzuela.monitoring.notification.channel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SlackNotificationChannel implements NotificationChannel {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public SlackNotificationChannel(RestClient restClient, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String channelType() {
        return "SLACK";
    }

    @Override
    public String displayName() {
        return "Slack";
    }

    @Override
    public void send(MonitoringEvent event, String configJson) {
        try {
            JsonNode config = jsonMapper.readTree(configJson);
            String webhookUrl = config.path("webhookUrl").asText("");

            if (webhookUrl.isBlank()) {
                log.debug("Slack notification skipped: no webhookUrl configured");
                return;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("text", buildMessage(event));

            restClient.post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Slack notification sent for service '{}'", serviceName(event));
        } catch (Exception e) {
            log.error("Failed to send Slack notification for service '{}'", serviceName(event), e);
        }
    }

    @Override
    public boolean validate(String configJson) {
        try {
            JsonNode config = jsonMapper.readTree(configJson);
            String webhookUrl = config.path("webhookUrl").asText("");
            return !webhookUrl.isBlank() && webhookUrl.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String configDescription() {
        return "JSON with keys: webhookUrl (required)";
    }

    @Override
    public List<ConfigField> configFields() {
        return List.of(
                new ConfigField("webhookUrl", "Webhook URL", true, "", "Slack incoming webhook URL", FieldType.SECRET)
        );
    }

    private String buildMessage(MonitoringEvent event) {
        return switch (event) {
            case ServiceHealthChangedEvent e -> e.wentDown()
                    ? ":red_circle: *Service DOWN:* %s (%s)\n_%s_".formatted(
                    serviceName(e), e.service().getUrl(), e.timestamp())
                    : ":large_green_circle: *Service UP:* %s (%s)\n_%s_".formatted(
                    serviceName(e), e.service().getUrl(), e.timestamp());
            case ServiceAddedEvent e ->
                    ":new: *Service Added:* %s (%s)\n_%s_".formatted(
                            serviceName(e), e.service().getUrl(), e.timestamp());
            case ServiceRemovedEvent e ->
                    ":wastebasket: *Service Removed:* %s (%s)\n_%s_".formatted(
                            serviceName(e), e.service().getUrl(), e.timestamp());
        };
    }

    private String serviceName(MonitoringEvent event) {
        MonitoredService service = event.service();
        return service.getName() != null ? service.getName() : service.getUrl();
    }
}
