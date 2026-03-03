package se.valenzuela.monitoring.core.auth;

/**
 * Sensitive credentials fetched from Vault for a monitored service.
 * Never persisted to the database.
 * <p>
 * Each field is only populated for its respective auth type:
 * <ul>
 *   <li>BASIC:         password</li>
 *   <li>BEARER_TOKEN:  token</li>
 *   <li>OAUTH2:        clientSecret</li>
 *   <li>MTLS:          certificatePem, privateKeyPem</li>
 * </ul>
 */
public record ServiceSecrets(
        String password,
        String token,
        String clientSecret,
        String certificatePem,
        String privateKeyPem
) {
    public static ServiceSecrets empty() {
        return new ServiceSecrets(null, null, null, null, null);
    }
}
