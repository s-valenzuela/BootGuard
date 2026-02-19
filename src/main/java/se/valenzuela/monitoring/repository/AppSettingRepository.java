package se.valenzuela.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.model.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
