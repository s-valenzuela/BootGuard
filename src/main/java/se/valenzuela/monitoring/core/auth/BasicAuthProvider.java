package se.valenzuela.monitoring.core.auth;

import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Adds an {@code Authorization: Basic …} header to every request. */
class BasicAuthProvider implements ServiceAuthProvider {

    private final String encodedCredentials;

    BasicAuthProvider(String username, String password) {
        String raw = username + ":" + (password != null ? password : "");
        this.encodedCredentials = Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public AuthType type() {
        return AuthType.BASIC;
    }

    @Override
    public void applyHeaders(RestClient.RequestHeadersSpec<?> request) {
        request.header("Authorization", "Basic " + encodedCredentials);
    }
}
