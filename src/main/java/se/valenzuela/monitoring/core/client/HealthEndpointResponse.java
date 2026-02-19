package se.valenzuela.monitoring.core.client;

import tools.jackson.databind.JsonNode;

import java.util.Map;

public record HealthEndpointResponse(String status, Map<String, JsonNode> components) {
}
