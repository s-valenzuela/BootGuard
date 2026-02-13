package se.valenzuela.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.valenzuela.monitoring.model.MonitoredService;

import java.util.Optional;

public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {

    Optional<MonitoredService> findByUrl(String url);

    boolean existsByUrl(String url);

    @Query("SELECT s FROM MonitoredService s LEFT JOIN FETCH s.environments WHERE s.id = :id")
    Optional<MonitoredService> findByIdWithEnvironments(Long id);
}
