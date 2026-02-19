package se.valenzuela.monitoring.notification.service;

import org.springframework.stereotype.Service;
import se.valenzuela.monitoring.notification.model.NotificationChannelConfig;
import se.valenzuela.monitoring.notification.model.NotificationServiceOverride;
import se.valenzuela.monitoring.notification.repository.NotificationChannelConfigRepository;
import se.valenzuela.monitoring.notification.repository.NotificationServiceOverrideRepository;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationConfigService {

    private final NotificationChannelConfigRepository channelConfigRepository;
    private final NotificationServiceOverrideRepository overrideRepository;

    public NotificationConfigService(NotificationChannelConfigRepository channelConfigRepository,
                                     NotificationServiceOverrideRepository overrideRepository) {
        this.channelConfigRepository = channelConfigRepository;
        this.overrideRepository = overrideRepository;
    }

    public boolean isEnabledForService(String channelType, Long serviceId) {
        Optional<NotificationServiceOverride> override =
                overrideRepository.findByServiceIdAndChannelType(serviceId, channelType);

        if (override.isPresent() && override.get().getEnabled() != null) {
            return override.get().getEnabled();
        }

        return channelConfigRepository.findByChannelType(channelType)
                .map(NotificationChannelConfig::isEnabled)
                .orElse(false);
    }

    public String getEffectiveConfigJson(String channelType, Long serviceId) {
        Optional<NotificationServiceOverride> override =
                overrideRepository.findByServiceIdAndChannelType(serviceId, channelType);

        if (override.isPresent() && override.get().getConfigJson() != null
                && !override.get().getConfigJson().isBlank()) {
            return override.get().getConfigJson();
        }

        return channelConfigRepository.findByChannelType(channelType)
                .map(NotificationChannelConfig::getConfigJson)
                .orElse("{}");
    }

    public Optional<NotificationChannelConfig> getGlobalConfig(String channelType) {
        return channelConfigRepository.findByChannelType(channelType);
    }

    public NotificationChannelConfig saveGlobalConfig(NotificationChannelConfig config) {
        return channelConfigRepository.save(config);
    }

    public Optional<NotificationServiceOverride> getOverride(Long serviceId, String channelType) {
        return overrideRepository.findByServiceIdAndChannelType(serviceId, channelType);
    }

    public NotificationServiceOverride saveOverride(NotificationServiceOverride override) {
        return overrideRepository.save(override);
    }

    public void deleteOverride(NotificationServiceOverride override) {
        overrideRepository.delete(override);
    }

    public List<NotificationServiceOverride> getOverridesForService(Long serviceId) {
        return overrideRepository.findByServiceId(serviceId);
    }
}
