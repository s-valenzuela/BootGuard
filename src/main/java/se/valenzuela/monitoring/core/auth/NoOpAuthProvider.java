package se.valenzuela.monitoring.core.auth;

/** Used when a service has no authentication configured. */
class NoOpAuthProvider implements ServiceAuthProvider {

    static final NoOpAuthProvider INSTANCE = new NoOpAuthProvider();

    private NoOpAuthProvider() {
    }

    @Override
    public AuthType type() {
        return AuthType.NONE;
    }
}
