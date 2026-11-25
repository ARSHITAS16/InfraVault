-- InfraVault Initial PostgreSQL Schema
-- Restored from authoritative JPA entity definitions

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'OPERATOR',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS datacenters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS datacenter_users (
    id BIGSERIAL PRIMARY KEY,
    datacenter_id BIGINT NOT NULL REFERENCES datacenters(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(255) NOT NULL,
    CONSTRAINT uk_datacenter_users_dc_user UNIQUE (datacenter_id, user_id)
);

CREATE TABLE IF NOT EXISTS folders (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    datacenter_id BIGINT NOT NULL REFERENCES datacenters(id) ON DELETE CASCADE,
    CONSTRAINT uk_folders_name_dc UNIQUE (name, datacenter_id)
);

CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    model VARCHAR(255),
    serial_number VARCHAR(255),
    capacity VARCHAR(255),
    service_tag VARCHAR(255),
    support_end_date DATE,
    console_port VARCHAR(255),
    idrac_configured BOOLEAN DEFAULT FALSE,
    folder_id BIGINT NOT NULL REFERENCES folders(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS credentials (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    type VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    encrypted_secret TEXT NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_fields (
    id BIGSERIAL PRIMARY KEY,
    field_name VARCHAR(255) NOT NULL,
    field_value TEXT,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255),
    entity_id BIGINT,
    datacenter_id BIGINT,
    timestamp TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    datacenter_id BIGINT NOT NULL REFERENCES datacenters(id) ON DELETE CASCADE,
    CONSTRAINT uk_locations_name_dc UNIQUE (name, datacenter_id)
);

CREATE TABLE IF NOT EXISTS infrastructure_folders (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    CONSTRAINT uk_infra_folders_name_loc UNIQUE (name, location_id)
);

CREATE TABLE IF NOT EXISTS infrastructure_hosts (
    id BIGSERIAL PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    folder_id BIGINT NOT NULL REFERENCES infrastructure_folders(id) ON DELETE CASCADE,
    CONSTRAINT uk_infra_hosts_hostname_folder UNIQUE (hostname, folder_id)
);

CREATE TABLE IF NOT EXISTS field_definitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    field_type VARCHAR(255) NOT NULL,
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    folder_id BIGINT NOT NULL REFERENCES infrastructure_folders(id) ON DELETE CASCADE,
    CONSTRAINT uk_field_defs_name_folder UNIQUE (name, folder_id)
);

CREATE TABLE IF NOT EXISTS field_values (
    id BIGSERIAL PRIMARY KEY,
    value TEXT,
    host_id BIGINT NOT NULL REFERENCES infrastructure_hosts(id) ON DELETE CASCADE,
    field_definition_id BIGINT NOT NULL REFERENCES field_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uk_field_vals_host_def UNIQUE (host_id, field_definition_id)
);

CREATE TABLE IF NOT EXISTS password_entries (
    id BIGSERIAL PRIMARY KEY,
    website_name VARCHAR(255) NOT NULL,
    website_url VARCHAR(255),
    username VARCHAR(255) NOT NULL,
    encrypted_password TEXT NOT NULL,
    notes TEXT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for common query paths
CREATE INDEX IF NOT EXISTS idx_datacenter_users_user_id ON datacenter_users(user_id);
CREATE INDEX IF NOT EXISTS idx_folders_datacenter_id ON folders(datacenter_id);
CREATE INDEX IF NOT EXISTS idx_devices_folder_id ON devices(folder_id);
CREATE INDEX IF NOT EXISTS idx_credentials_device_id ON credentials(device_id);
CREATE INDEX IF NOT EXISTS idx_device_fields_device_id ON device_fields(device_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_datacenter_id ON audit_logs(datacenter_id);
