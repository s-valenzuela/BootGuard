package se.valenzuela.monitoring.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.notification.config.NotificationChannelConfig;

import java.util.Optional;

public interface NotificationChannelConfigRepository extends JpaRepository<NotificationChannelConfig, Long> {

    Optional<NotificationChannelConfig> findByChannelType(String channelType);
}
