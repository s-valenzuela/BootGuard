package se.valenzuela.monitoring.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.core.model.Environment;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    Optional<Environment> findByName(String name);

    boolean existsByName(String name);

    List<Environment> findAllByOrderByDisplayOrderAscNameAsc();
}
