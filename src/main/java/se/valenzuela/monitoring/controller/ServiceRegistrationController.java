package se.valenzuela.monitoring.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import se.valenzuela.monitoring.service.MonitoringService;

@Slf4j
@RestController
public class ServiceRegistrationController {

    private final MonitoringService monitoringService;

    public ServiceRegistrationController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @PostMapping("/register")
    public void register(@Valid @RequestBody ServiceRegistrationRecord service) {
        log.info("Register service: {}", service);
        monitoringService.addService(service.url());
    }

}
