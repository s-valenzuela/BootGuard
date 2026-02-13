CREATE TABLE environment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7),
    display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE service_environment (
    service_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    PRIMARY KEY (service_id, environment_id),
    CONSTRAINT fk_se_service FOREIGN KEY (service_id) REFERENCES monitored_service(id) ON DELETE CASCADE,
    CONSTRAINT fk_se_environment FOREIGN KEY (environment_id) REFERENCES environment(id) ON DELETE CASCADE
);
