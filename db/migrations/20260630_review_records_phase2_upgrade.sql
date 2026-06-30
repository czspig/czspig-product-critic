-- Upgrade an existing czspig_product_critic database to the current
-- pre-phase-2 review_records shape.
--
-- Run after taking a backup:
--   mysqldump -u <user> -p czspig_product_critic > backup.sql
--   mysql -u <user> -p czspig_product_critic < db/migrations/20260630_review_records_phase2_upgrade.sql
--
-- The ADD COLUMN statements are intentionally separated because MySQL versions
-- differ in IF NOT EXISTS support for ALTER TABLE. If a column already exists,
-- skip that statement and continue with the remaining statements.

USE czspig_product_critic;

ALTER TABLE review_records
  ADD COLUMN idea_group_id VARCHAR(64) NULL COMMENT 'Idea iteration group id' AFTER input_summary;

ALTER TABLE review_records
  ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT 'Idea iteration version number' AFTER idea_group_id;

ALTER TABLE review_records
  ADD COLUMN parent_review_id BIGINT NULL COMMENT 'Parent review record id for an iteration' AFTER version_no;

ALTER TABLE review_records
  ADD COLUMN positioning_score TINYINT NOT NULL DEFAULT 0 COMMENT 'Product positioning score, 0-100' AFTER beat_score;

ALTER TABLE review_records
  ADD COLUMN error_message VARCHAR(512) NULL COMMENT 'Sanitized failure reason' AFTER status;

ALTER TABLE review_records
  ADD COLUMN prompt_version VARCHAR(32) NOT NULL DEFAULT 'mvp-v1' COMMENT 'Prompt version' AFTER model_name;

CREATE INDEX idx_review_records_group_version
  ON review_records (idea_group_id, version_no);

CREATE INDEX idx_review_records_parent_review_id
  ON review_records (parent_review_id);

CREATE INDEX idx_review_records_status
  ON review_records (status);

UPDATE review_records
SET idea_group_id = CAST(id AS CHAR),
    version_no = COALESCE(NULLIF(version_no, 0), 1)
WHERE idea_group_id IS NULL OR idea_group_id = '';

UPDATE review_records
SET positioning_score = CASE
  WHEN positioning_score < 0 THEN 0
  WHEN positioning_score > 100 THEN 100
  ELSE positioning_score
END;
