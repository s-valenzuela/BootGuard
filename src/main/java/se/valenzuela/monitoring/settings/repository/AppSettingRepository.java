package se.valenzuela.monitoring.settings.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.settings.model.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
