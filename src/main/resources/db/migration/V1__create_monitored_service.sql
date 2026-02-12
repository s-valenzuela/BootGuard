CREATE TABLE monitored_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(2048) NOT NULL UNIQUE,
    name VARCHAR(255),
    version VARCHAR(255),
    info_status BOOLEAN NOT NULL DEFAULT FALSE,
    health_status BOOLEAN NOT NULL DEFAULT FALSE,
    health_response_status VARCHAR(255),
    last_updated TIMESTAMP(6),
    info_endpoint VARCHAR(255) NOT NULL,
    health_endpoint VARCHAR(255) NOT NULL
);
