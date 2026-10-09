CREATE TABLE IF NOT EXISTS anonymous_project_migrations (
 anonymous_owner VARCHAR(100) NOT NULL,
 project_id VARCHAR(64) NOT NULL,
 account_id BIGINT NOT NULL,
 migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(anonymous_owner,project_id), INDEX migration_account(account_id)
);
INSERT IGNORE INTO account_schema_versions(version) VALUES ('accounts-v2-migration-receipts');
