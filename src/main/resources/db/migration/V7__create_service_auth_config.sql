CREATE TABLE service_auth_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id  BIGINT       NOT NULL UNIQUE,
    auth_type   VARCHAR(30)  NOT NULL DEFAULT 'NONE',
    config_json LONGTEXT,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_auth_service
        FOREIGN KEY (service_id) REFERENCES monitored_service (id)
        ON DELETE CASCADE
);
