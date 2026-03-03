package se.valenzuela.monitoring.core.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Fetches an access token from a token endpoint using the OAuth 2.0
 * client_credentials grant, then adds {@code Authorization: Bearer …} to
 * every outgoing request.
 *
 * <p>The token is cached until it expires (minus a 30-second safety buffer)
 * so that BootGuard does not hit the token endpoint on every health check poll.
 * To force a token refresh after a client-secret rotation, evict this service
 * from {@link ServiceAuthProviderFactory}.
 */
@Slf4j
class OAuth2ClientCredentialsProvider implements ServiceAuthProvider {

    private static final int EXPIRY_BUFFER_SECONDS = 30;

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final RestClient tokenClient;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    OAuth2ClientCredentialsProvider(String tokenUrl, String clientId,
                                    String clientSecret, String scope) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenClient = RestClient.create();
    }

    @Override
    public AuthType type() {
        return AuthType.OAUTH2;
    }

    @Override
    public void applyHeaders(RestClient.RequestHeadersSpec<?> request) {
        request.header("Authorization", "Bearer " + accessToken());
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        TokenResponse response = tokenClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(buildForm())
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.access_token() == null) {
            throw new IllegalStateException("Token endpoint returned no access_token");
        }
        cachedToken = response.access_token();
        long expiresIn = response.expires_in() != null ? response.expires_in() : 3600L;
        tokenExpiry = Instant.now().plusSeconds(expiresIn - EXPIRY_BUFFER_SECONDS);
        log.debug("Fetched OAuth2 token for client '{}', expires in {}s", clientId, expiresIn);
        return cachedToken;
    }

    private String buildForm() {
        StringBuilder form = new StringBuilder("grant_type=client_credentials")
                .append("&client_id=").append(encode(clientId))
                .append("&client_secret=").append(encode(clientSecret));
        if (scope != null && !scope.isBlank()) {
            form.append("&scope=").append(encode(scope));
        }
        return form.toString();
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @SuppressWarnings("unused")
    private record TokenResponse(String access_token, Long expires_in, String token_type) {}
}
