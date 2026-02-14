package se.valenzuela.monitoring.notification.channel;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.ServiceAddedEvent;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(mailSender, JsonMapper.builder().build());
    }

    private MonitoredService createService() {
        var service = new MonitoredService("http://localhost:8080");
        service.setName("test-app");
        return service;
    }

    @Test
    void validConfig_sendsEmail() {
        var event = new ServiceHealthChangedEvent(createService(), true, false, Instant.now());
        String config = """
                {"recipients":"admin@example.com","fromAddress":"bootguard@localhost","subjectPrefix":"[BG]"}""";

        channel.send(event, config);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("bootguard@localhost", msg.getFrom());
        assertArrayEquals(new String[]{"admin@example.com"}, msg.getTo());
        assertTrue(msg.getSubject().contains("[BG]"));
        assertTrue(msg.getSubject().contains("DOWN"));
        assertTrue(msg.getText().contains("test-app"));
    }

    @Test
    void serviceUp_sendsUpEmail() {
        var event = new ServiceHealthChangedEvent(createService(), false, true, Instant.now());
        String config = """
                {"recipients":"admin@example.com","fromAddress":"bootguard@localhost","subjectPrefix":"[BG]"}""";

        channel.send(event, config);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("UP"));
    }

    @Test
    void serviceAdded_sendsAddedEmail() {
        var event = new ServiceAddedEvent(createService(), Instant.now());
        String config = """
                {"recipients":"admin@example.com","fromAddress":"bootguard@localhost","subjectPrefix":"[BG]"}""";

        channel.send(event, config);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Added"));
    }

    @Test
    void emptyRecipients_skips() {
        var event = new ServiceHealthChangedEvent(createService(), true, false, Instant.now());
        String config = """
                {"recipients":"","fromAddress":"bootguard@localhost","subjectPrefix":"[BG]"}""";

        channel.send(event, config);

        verifyNoInteractions(mailSender);
    }

    @Test
    void mailException_loggedNotThrown() {
        var event = new ServiceHealthChangedEvent(createService(), true, false, Instant.now());
        String config = """
                {"recipients":"admin@example.com","fromAddress":"bootguard@localhost","subjectPrefix":"[BG]"}""";

        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> channel.send(event, config));
    }

    @Test
    void validate_validConfig_returnsTrue() {
        assertTrue(channel.validate("""
                {"recipients":"a@b.com","fromAddress":"x@y.com"}"""));
    }

    @Test
    void validate_missingKeys_returnsFalse() {
        assertFalse(channel.validate("{}"));
    }

    @Test
    void validate_invalidJson_returnsFalse() {
        assertFalse(channel.validate("not json"));
    }
}
