CREATE DATABASE IF NOT EXISTS czspig_product_critic
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE czspig_product_critic;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  username VARCHAR(64) NULL COMMENT '用户名，第一版预留',
  display_name VARCHAR(64) NULL COMMENT '展示名',
  avatar_url VARCHAR(512) NULL COMMENT '头像地址',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表，第一版仅预留';

CREATE TABLE IF NOT EXISTS review_records (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评审记录 ID',
  user_id BIGINT NULL COMMENT '用户 ID，第一版可为空',
  session_id VARCHAR(128) NOT NULL COMMENT '匿名会话 ID',
  input_content MEDIUMTEXT NOT NULL COMMENT '用户输入的产品想法或需求内容',
  input_summary VARCHAR(255) NOT NULL COMMENT '输入摘要',
  idea_group_id VARCHAR(64) NULL COMMENT '同一个产品想法的迭代组',
  version_no INT NOT NULL DEFAULT 1 COMMENT '想法迭代版本号',
  parent_review_id BIGINT NULL COMMENT '当前版本基于哪一次评审生成',
  mode VARCHAR(32) NOT NULL COMMENT '评审模式：MENTOR/SHARP_PM/CLIENT',
  roast_level TINYINT NOT NULL COMMENT '吐槽强度：1 温和，2 正常，3 毒舌',
  one_line_verdict VARCHAR(255) NOT NULL COMMENT '一句话评价',
  beat_score TINYINT NOT NULL COMMENT '毒打指数，0-100',
  positioning_score TINYINT NOT NULL COMMENT '产品定位评分，0-100',
  report_json JSON NOT NULL COMMENT '结构化评审报告',
  report_markdown MEDIUMTEXT NOT NULL COMMENT 'Markdown 评审报告',
  status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT 'PENDING/SUCCESS/FAILED',
  error_message VARCHAR(512) NULL COMMENT '脱敏后的失败原因',
  model_name VARCHAR(128) NOT NULL DEFAULT 'mock-product-reviewer-v1' COMMENT '模型名称，支持 deepseek 或 mock',
  prompt_version VARCHAR(32) NOT NULL DEFAULT 'mvp-v1' COMMENT 'Prompt 版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at DATETIME NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_review_records_session_created (session_id, created_at),
  KEY idx_review_records_group_version (idea_group_id, version_no),
  KEY idx_review_records_parent_review_id (parent_review_id),
  KEY idx_review_records_user_created (user_id, created_at),
  KEY idx_review_records_status (status),
  CONSTRAINT chk_review_records_roast_level CHECK (roast_level BETWEEN 1 AND 3),
  CONSTRAINT chk_review_records_beat_score CHECK (beat_score BETWEEN 0 AND 100),
  CONSTRAINT chk_review_records_positioning_score CHECK (positioning_score BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品评审记录';

CREATE TABLE IF NOT EXISTS ai_call_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 调用日志 ID',
  review_record_id BIGINT NOT NULL COMMENT '关联评审记录 ID',
  provider VARCHAR(64) NOT NULL COMMENT 'AI Provider，如 deepseek/mock',
  model_name VARCHAR(128) NOT NULL COMMENT '模型名称',
  request_id VARCHAR(128) NULL COMMENT '服务商请求 ID',
  prompt_hash VARCHAR(128) NULL COMMENT 'Prompt 哈希，避免保存完整系统提示词',
  request_summary TEXT NULL COMMENT '脱敏请求摘要',
  response_summary TEXT NULL COMMENT '脱敏响应摘要',
  input_tokens INT NOT NULL DEFAULT 0 COMMENT '输入 token 数，未接 usage 时为 0',
  output_tokens INT NOT NULL DEFAULT 0 COMMENT '输出 token 数，未接 usage 时为 0',
  latency_ms INT NOT NULL DEFAULT 0 COMMENT '调用耗时',
  status VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILED',
  error_code VARCHAR(128) NULL COMMENT '错误码',
  error_message VARCHAR(512) NULL COMMENT '脱敏错误信息',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ai_call_logs_review_record_id (review_record_id),
  KEY idx_ai_call_logs_created_at (created_at),
  CONSTRAINT fk_ai_call_logs_review_record
    FOREIGN KEY (review_record_id) REFERENCES review_records (id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 调用日志';
