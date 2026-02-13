package se.valenzuela.monitoring.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.valenzuela.monitoring.model.Environment;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.repository.EnvironmentRepository;
import se.valenzuela.monitoring.repository.MonitoredServiceRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final MonitoredServiceRepository serviceRepository;

    public EnvironmentService(EnvironmentRepository environmentRepository,
                              MonitoredServiceRepository serviceRepository) {
        this.environmentRepository = environmentRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<Environment> getAllEnvironments() {
        return environmentRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public Environment createEnvironment(String name, String color, int displayOrder) {
        var environment = new Environment(name, color, displayOrder);
        return environmentRepository.save(environment);
    }

    public Environment updateEnvironment(Environment environment) {
        return environmentRepository.save(environment);
    }

    public void deleteEnvironment(Environment environment) {
        environmentRepository.delete(environment);
    }

    public boolean existsByName(String name) {
        return environmentRepository.existsByName(name);
    }

    @Transactional
    public void updateServiceEnvironments(MonitoredService service, Set<Environment> environments) {
        MonitoredService managed = serviceRepository.findByIdWithEnvironments(service.getId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + service.getId()));
        managed.getEnvironments().clear();
        managed.getEnvironments().addAll(environments);
        serviceRepository.save(managed);
    }

    @Transactional(readOnly = true)
    public Set<Environment> getEnvironmentsForService(Long serviceId) {
        return serviceRepository.findByIdWithEnvironments(serviceId)
                .map(s -> new HashSet<>(s.getEnvironments()))
                .orElse(new HashSet<>());
    }
}
