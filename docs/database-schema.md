# 数据库设计

初始化脚本：[db/schema.sql](../db/schema.sql)

默认数据库：`czspig_product_critic`

## 设计原则

- 第一版不实现完整登录，`users` 表只预留。
- Review 历史通过 `session_id` 做匿名隔离。
- `review_records` 保存完整用户输入、结构化报告和 Markdown 报告；失败时保存脱敏错误信息。
- `ai_call_logs` 只保存 Prompt 哈希、脱敏摘要、模型名、状态和耗时，不保存真实 API Key 或完整系统提示词。

## users

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 用户 ID |
| username | VARCHAR(64) | 用户名，第一版预留 |
| display_name | VARCHAR(64) | 展示名 |
| avatar_url | VARCHAR(512) | 头像地址 |
| status | VARCHAR(32) | 用户状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## review_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 评审记录 ID |
| user_id | BIGINT | 用户 ID，第一版可为空 |
| session_id | VARCHAR(128) | 匿名会话 ID |
| input_content | MEDIUMTEXT | 用户原始输入 |
| input_summary | VARCHAR(255) | 输入摘要 |
| idea_group_id | VARCHAR(64) | 同一个产品想法的迭代组；旧数据为空时按自身 `id` 兜底 |
| version_no | INT | 想法版本号，默认 1 |
| parent_review_id | BIGINT | 当前版本基于哪一次评审生成，第一版为空 |
| mode | VARCHAR(32) | `MENTOR` / `SHARP_PM` / `CLIENT` |
| roast_level | TINYINT | 1 温和，2 正常，3 毒舌 |
| one_line_verdict | VARCHAR(255) | 一句话评价 |
| beat_score | TINYINT | 毒打指数，0-100 |
| positioning_score | TINYINT | 产品定位评分，0-100 |
| report_json | JSON | 结构化报告 |
| report_markdown | MEDIUMTEXT | Markdown 报告 |
| status | VARCHAR(32) | `PENDING` / `SUCCESS` / `FAILED` |
| error_message | VARCHAR(512) | 脱敏错误信息 |
| model_name | VARCHAR(128) | `deepseek-v4-flash` / `mock-product-reviewer-v1` / 其他配置模型 |
| prompt_version | VARCHAR(32) | 当前为 `mvp-v1` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 软删除时间 |

索引：

- `idx_review_records_session_created`
- `idx_review_records_group_version`
- `idx_review_records_parent_review_id`
- `idx_review_records_user_created`
- `idx_review_records_status`

### 旧数据库升级 SQL

如果本地或服务器已经初始化过旧版数据库，需要手动执行：

```sql
ALTER TABLE review_records
  ADD COLUMN idea_group_id VARCHAR(64) NULL COMMENT '同一个产品想法的迭代组',
  ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT '想法迭代版本号',
  ADD COLUMN parent_review_id BIGINT NULL COMMENT '当前版本基于哪一次评审生成';

CREATE INDEX idx_review_records_group_version
  ON review_records (idea_group_id, version_no);

CREATE INDEX idx_review_records_parent_review_id
  ON review_records (parent_review_id);
```

旧数据没有 `idea_group_id` 时，后端会按该条记录自身 `id` 作为轻量兜底组 ID；新建 V1 会在插入后回填 `idea_group_id = id`。

## ai_call_logs

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 日志 ID |
| review_record_id | BIGINT | 关联评审记录 |
| provider | VARCHAR(64) | `deepseek` / `mock` / `unknown` |
| model_name | VARCHAR(128) | 模型名 |
| request_id | VARCHAR(128) | 服务商请求 ID，当前预留 |
| prompt_hash | VARCHAR(128) | Prompt 哈希 |
| request_summary | TEXT | 脱敏请求摘要 |
| response_summary | TEXT | 脱敏响应摘要 |
| input_tokens | INT | 输入 token，当前未接 token usage 时为 0 |
| output_tokens | INT | 输出 token，当前未接 token usage 时为 0 |
| latency_ms | INT | 调用耗时 |
| status | VARCHAR(32) | `SUCCESS` / `FAILED` |
| error_code | VARCHAR(128) | 错误码 |
| error_message | VARCHAR(512) | 脱敏错误信息 |
| created_at | DATETIME | 创建时间 |

## 初始化方式

```bash
mysql -u root -p < db/schema.sql
```

脚本默认使用 `utf8mb4_unicode_ci`，兼容 MySQL 5.7/8.0 的常见本地开发环境。
