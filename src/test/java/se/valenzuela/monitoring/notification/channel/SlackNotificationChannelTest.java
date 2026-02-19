package se.valenzuela.monitoring.notification.channel;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.ServiceAddedEvent;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackNotificationChannelTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private SlackNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new SlackNotificationChannel(restClient, JsonMapper.builder().build());
    }

    private MonitoredService createService() {
        var service = new MonitoredService("http://localhost:8080");
        service.setName("test-app");
        return service;
    }

    @SuppressWarnings("unchecked")
    private void stubRestClient() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Map.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
    }

    @Test
    void validConfig_sendsSlackMessage() {
        stubRestClient();
        var event = new ServiceHealthChangedEvent(createService(), true, false, Instant.now());
        String config = """
                {"webhookUrl":"https://hooks.slack.com/services/T00/B00/xxx"}""";

        channel.send(event, config);

        verify(requestBodyUriSpec).uri("https://hooks.slack.com/services/T00/B00/xxx");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec).body(captor.capture());

        Map<String, Object> payload = captor.getValue();
        String text = (String) payload.get("text");
        assertTrue(text.contains("DOWN"));
        assertTrue(text.contains("test-app"));
    }

    @Test
    void serviceUp_sendsUpMessage() {
        stubRestClient();
        var event = new ServiceHealthChangedEvent(createService(), false, true, Instant.now());
        String config = """
                {"webhookUrl":"https://hooks.slack.com/services/T00/B00/xxx"}""";

        channel.send(event, config);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec).body(captor.capture());
        assertTrue(((String) captor.getValue().get("text")).contains("UP"));
    }

    @Test
    void serviceAdded_sendsAddedMessage() {
        stubRestClient();
        var event = new ServiceAddedEvent(createService(), Instant.now());
        String config = """
                {"webhookUrl":"https://hooks.slack.com/services/T00/B00/xxx"}""";

        channel.send(event, config);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec).body(captor.capture());
        assertTrue(((String) captor.getValue().get("text")).contains("Added"));
    }

    @Test
    void emptyWebhookUrl_skips() {
        var event = new ServiceHealthChangedEvent(createService(), true, false, Instant.now());
        String config = """
                {"webhookUrl":""}""";

        channel.send(event, config);

        verifyNoInteractions(restClient);
    }

    @Test
    void restClientException_loggedNotThrown() {
        when(restClient.post()).thenThrow(new RuntimeException("Connection refused"));
        var event = new ServiceHealthChangedEvent(createService(), true, false, Instant.now());
        String config = """
                {"webhookUrl":"https://hooks.slack.com/services/T00/B00/xxx"}""";

        assertDoesNotThrow(() -> channel.send(event, config));
    }

    @Test
    void validate_validConfig_returnsTrue() {
        assertTrue(channel.validate("""
                {"webhookUrl":"https://hooks.slack.com/services/T00/B00/xxx"}"""));
    }

    @Test
    void validate_httpUrl_returnsFalse() {
        assertFalse(channel.validate("""
                {"webhookUrl":"http://hooks.slack.com/services/T00/B00/xxx"}"""));
    }

    @Test
    void validate_missingWebhookUrl_returnsFalse() {
        assertFalse(channel.validate("{}"));
    }

    @Test
    void validate_invalidJson_returnsFalse() {
        assertFalse(channel.validate("not json"));
    }
}
