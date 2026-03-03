package se.valenzuela.monitoring.core.auth;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import se.valenzuela.monitoring.config.SslContextFactory;

import javax.net.ssl.SSLContext;
import java.util.Optional;

/**
 * Provides a dedicated {@link ClientHttpRequestFactory} whose underlying Apache
 * HttpClient carries the service's client certificate for mutual TLS.
 *
 * <p>The factory (and its connection pool) is built once and reused for all
 * requests to this service. The server-side trust follows the same composite
 * strategy as the shared client — trusting both public CAs and the application's
 * self-signed bundle.
 */
class MtlsAuthProvider implements ServiceAuthProvider {

    private final ClientHttpRequestFactory factory;

    MtlsAuthProvider(String certificatePem, String privateKeyPem,
                     SslContextFactory sslContextFactory) {
        SSLContext sslContext = sslContextFactory.buildMtlsSslContext(certificatePem, privateKeyPem);
        var tlsStrategy = new DefaultClientTlsStrategy(sslContext);
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .build();
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        this.factory = new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Override
    public AuthType type() {
        return AuthType.MTLS;
    }

    @Override
    public Optional<ClientHttpRequestFactory> requestFactory() {
        return Optional.of(factory);
    }
}
