package se.valenzuela.monitoring.config;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Trusts certificates accepted by any of the provided trust managers.
 * Used to combine the JVM default CA store with a custom (self-signed) trust store.
 */
class CompositeTrustManager implements X509TrustManager {

    private final X509TrustManager[] delegates;

    CompositeTrustManager(X509TrustManager... delegates) {
        this.delegates = delegates;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        CertificateException last = null;
        for (X509TrustManager tm : delegates) {
            try {
                tm.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
                last = e;
            }
        }
        throw last;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        CertificateException last = null;
        for (X509TrustManager tm : delegates) {
            try {
                tm.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
                last = e;
            }
        }
        throw last;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        java.util.List<X509Certificate> certs = new java.util.ArrayList<>();
        for (X509TrustManager tm : delegates) {
            certs.addAll(java.util.Arrays.asList(tm.getAcceptedIssuers()));
        }
        return certs.toArray(new X509Certificate[0]);
    }
}
