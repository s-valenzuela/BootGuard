package se.valenzuela.monitoring.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.notification.model.NotificationServiceOverride;

import java.util.List;
import java.util.Optional;

public interface NotificationServiceOverrideRepository extends JpaRepository<NotificationServiceOverride, Long> {

    Optional<NotificationServiceOverride> findByServiceIdAndChannelType(Long serviceId, String channelType);

    List<NotificationServiceOverride> findByServiceId(Long serviceId);
}
