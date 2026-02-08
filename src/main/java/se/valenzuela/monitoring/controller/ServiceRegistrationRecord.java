package se.valenzuela.monitoring.controller;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ServiceRegistrationRecord(
        @NotBlank @URL String url) {

}
