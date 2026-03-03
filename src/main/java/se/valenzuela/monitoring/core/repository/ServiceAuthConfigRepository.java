package se.valenzuela.monitoring.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.core.model.ServiceAuthConfig;

import java.util.Optional;

public interface ServiceAuthConfigRepository extends JpaRepository<ServiceAuthConfig, Long> {

    Optional<ServiceAuthConfig> findByServiceId(Long serviceId);

    void deleteByServiceId(Long serviceId);
}
