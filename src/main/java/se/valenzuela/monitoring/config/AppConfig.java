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

import javax.net.ssl.SSLContext;


@Configuration
@EnableScheduling
@EnableAsync
public class AppConfig {

    @Bean
    public RestClient restClient(SslBundles sslBundles) {
        SSLContext sslContext = sslBundles.getBundle("server").createSslContext();
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

}
