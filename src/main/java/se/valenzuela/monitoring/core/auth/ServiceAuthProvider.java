package se.valenzuela.monitoring.core.auth;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Strategy that applies authentication to outgoing requests for a specific monitored service.
 *
 * <p>For header-based auth (BASIC, BEARER_TOKEN, OAUTH2) use {@link #applyHeaders}.
 * For mTLS, {@link #requestFactory()} returns a factory whose underlying {@code HttpClient}
 * carries the service's client certificate — the caller must build a dedicated
 * {@code RestClient} from it instead of using the shared one.
 */
public interface ServiceAuthProvider {

    /** Auth type this provider handles. */
    AuthType type();

    /**
     * Adds auth headers to an in-flight RestClient request spec.
     * Implementations call {@code request.header(...)} one or more times.
     * The default is a no-op.
     */
    default void applyHeaders(RestClient.RequestHeadersSpec<?> request) {
    }

    /**
     * Returns a {@code ClientHttpRequestFactory} configured for this service's TLS
     * client certificate, or {@link Optional#empty()} for non-mTLS auth types.
     * When non-empty, callers must build a dedicated {@code RestClient} from this
     * factory instead of using the shared one.
     */
    default Optional<ClientHttpRequestFactory> requestFactory() {
        return Optional.empty();
    }
}
