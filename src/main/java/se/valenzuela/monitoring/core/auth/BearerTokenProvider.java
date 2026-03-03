package se.valenzuela.monitoring.core.auth;

import org.springframework.web.client.RestClient;

/** Adds a static {@code Authorization: Bearer …} header to every request. */
class BearerTokenProvider implements ServiceAuthProvider {

    private final String token;

    BearerTokenProvider(String token) {
        this.token = token;
    }

    @Override
    public AuthType type() {
        return AuthType.BEARER_TOKEN;
    }

    @Override
    public void applyHeaders(RestClient.RequestHeadersSpec<?> request) {
        request.header("Authorization", "Bearer " + token);
    }
}
