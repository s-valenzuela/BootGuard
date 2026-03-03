ALTER TABLE environment
    ADD COLUMN default_auth_type         VARCHAR(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN default_auth_config_json  LONGTEXT    DEFAULT NULL;
