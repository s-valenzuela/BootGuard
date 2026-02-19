package se.valenzuela.monitoring.settings.service;

import org.springframework.stereotype.Service;
import se.valenzuela.monitoring.settings.model.AppSetting;
import se.valenzuela.monitoring.settings.repository.AppSettingRepository;

@Service
public class AppSettingService {

    public static final String CERT_EXPIRY_WARNING_DAYS = "cert.expiry.warning.days";
    public static final int DEFAULT_CERT_EXPIRY_WARNING_DAYS = 30;

    private final AppSettingRepository repository;

    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public String getValue(String key, String defaultValue) {
        return repository.findById(key)
                .map(AppSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public void setValue(String key, String value) {
        AppSetting setting = repository.findById(key)
                .orElse(new AppSetting(key, value));
        setting.setSettingValue(value);
        repository.save(setting);
    }

    public int getCertExpiryWarningDays() {
        String value = getValue(CERT_EXPIRY_WARNING_DAYS, String.valueOf(DEFAULT_CERT_EXPIRY_WARNING_DAYS));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return DEFAULT_CERT_EXPIRY_WARNING_DAYS;
        }
    }
}
