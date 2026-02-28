ALTER TABLE environment ADD COLUMN health_check_interval_seconds INT DEFAULT NULL;
ALTER TABLE monitored_service ADD COLUMN health_check_interval_seconds INT DEFAULT NULL;
