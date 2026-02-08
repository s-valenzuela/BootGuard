package se.valenzuela.monitoring.client;

public record InfoEndpointResponse(String name, String description, String version) {

    public InfoEndpointResponse() {
        this("Unknown", "Unknown", "Unknown");
    }
}
