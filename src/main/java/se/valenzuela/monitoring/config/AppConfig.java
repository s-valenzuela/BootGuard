package se.valenzuela.monitoring.config;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


@Configuration
@EnableScheduling
@EnableAsync
public class AppConfig {

    @Bean
    public RestClient restClient(SslBundles sslBundles) throws NoSuchAlgorithmException, KeyStoreException {
        SSLContext sslContext = buildCompositeSslContext(sslBundles);
        var tlsStrategy = new DefaultClientTlsStrategy(sslContext);
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .build();
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Builds an SSLContext whose trust manager accepts certificates trusted by either:
     * - the JVM's default CA store (covers public CAs like Let's Encrypt, DigiCert, etc.), or
     * - the application's custom SSL bundle (covers self-signed certs of monitored services).
     */
    private SSLContext buildCompositeSslContext(SslBundles sslBundles)
            throws NoSuchAlgorithmException, KeyStoreException {
        // Trust manager from the JVM default CA store
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null);
        X509TrustManager defaultTm = findX509TrustManager(defaultTmf);

        // Trust manager from the Spring SSL bundle (self-signed cert)
        TrustManagerFactory bundleTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        bundleTmf.init(sslBundles.getBundle("server").getStores().getTrustStore());
        X509TrustManager bundleTm = findX509TrustManager(bundleTmf);

        X509TrustManager compositeTm = new CompositeTrustManager(defaultTm, bundleTm);

        try {
            KeyManager[] keyManagers = sslBundles.getBundle("server").getManagers().getKeyManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, new TrustManager[]{compositeTm}, null);
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build composite SSL context", e);
        }
    }

    private X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
        return Arrays.stream(tmf.getTrustManagers())
                .filter(tm -> tm instanceof X509TrustManager)
                .map(tm -> (X509TrustManager) tm)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No X509TrustManager found"));
    }

}
