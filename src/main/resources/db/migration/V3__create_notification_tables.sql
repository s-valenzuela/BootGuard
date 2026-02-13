CREATE TABLE notification_channel_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_type VARCHAR(50) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    config_json TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE notification_service_override (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN,
    config_json TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_override_service FOREIGN KEY (service_id) REFERENCES monitored_service(id) ON DELETE CASCADE,
    CONSTRAINT uq_service_channel UNIQUE (service_id, channel_type)
);

INSERT INTO notification_channel_config (channel_type, enabled, config_json)
VALUES ('EMAIL', FALSE, '{"recipients":"","fromAddress":"bootguard@localhost","subjectPrefix":"[BootGuard]"}');
