package se.valenzuela.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.model.MonitoredService;

import java.util.Optional;

public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {

    Optional<MonitoredService> findByUrl(String url);

    boolean existsByUrl(String url);
}
