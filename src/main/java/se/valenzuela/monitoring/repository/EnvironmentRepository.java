package se.valenzuela.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.valenzuela.monitoring.model.Environment;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    Optional<Environment> findByName(String name);

    boolean existsByName(String name);

    List<Environment> findAllByOrderByDisplayOrderAscNameAsc();
}
