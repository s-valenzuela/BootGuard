package se.valenzuela.monitoring.config;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Centralises SSL context creation for outgoing HTTP connections.
 *
 * <p>All outgoing clients — both the shared monitoring client and per-service
 * mTLS clients — use the same composite trust model: certificates trusted by
 * either the JVM's default CA store or the application's self-signed bundle
 * are accepted. This lets BootGuard connect to services using public CAs,
 * private CAs, and self-signed certificates without extra configuration.
 */
@Component
public class SslContextFactory {

    private final SslBundles sslBundles;

    /** Lazily initialised and cached once built. */
    private volatile X509TrustManager compositeTrustManager;

    public SslContextFactory(SslBundles sslBundles) {
        this.sslBundles = sslBundles;
    }

    /**
     * Builds the default outgoing SSLContext used by the shared {@code RestClient}.
     * Uses the server bundle's key managers so that BootGuard can present its own
     * certificate when connecting to services that require client-auth.
     */
    public SSLContext buildDefaultSslContext() {
        try {
            KeyManager[] keyManagers = sslBundles.getBundle("server").getManagers().getKeyManagers();
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagers, new TrustManager[]{getCompositeTrustManager()}, null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build default SSL context", e);
        }
    }

    /**
     * Builds a per-service SSLContext for mutual TLS. The client certificate and
     * private key are supplied as PEM text fetched from Vault — no files or SSL
     * bundles are involved.
     *
     * <p>Both RSA and EC private keys (PKCS#8 format) are supported.
     */
    public SSLContext buildMtlsSslContext(String certificatePem, String privateKeyPem) {
        try {
            X509Certificate cert = parseCertificate(certificatePem);
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry("client", privateKey, new char[0], new Certificate[]{cert});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, new char[0]);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), new TrustManager[]{getCompositeTrustManager()}, null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build mTLS SSL context", e);
        }
    }

    /** Returns the shared composite trust manager, building it on first call. */
    X509TrustManager getCompositeTrustManager() {
        if (compositeTrustManager == null) {
            synchronized (this) {
                if (compositeTrustManager == null) {
                    compositeTrustManager = buildCompositeTrustManager();
                }
            }
        }
        return compositeTrustManager;
    }

    private X509TrustManager buildCompositeTrustManager() {
        try {
            TrustManagerFactory defaultTmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTmf.init((KeyStore) null);
            X509TrustManager defaultTm = findX509TrustManager(defaultTmf);

            TrustManagerFactory bundleTmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            bundleTmf.init(sslBundles.getBundle("server").getStores().getTrustStore());
            X509TrustManager bundleTm = findX509TrustManager(bundleTmf);

            return new CompositeTrustManager(defaultTm, bundleTm);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build composite trust manager", e);
        }
    }

    private X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
        return Arrays.stream(tmf.getTrustManagers())
                .filter(tm -> tm instanceof X509TrustManager)
                .map(tm -> (X509TrustManager) tm)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No X509TrustManager found"));
    }

    private X509Certificate parseCertificate(String pem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }

    private PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        // Try RSA first, then EC
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        }
    }
}
