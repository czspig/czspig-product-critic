# 数据库设计

初始化脚本：[db/schema.sql](../db/schema.sql)

旧库升级脚本：[db/migrations/20260630_review_records_phase2_upgrade.sql](../db/migrations/20260630_review_records_phase2_upgrade.sql)

默认数据库：`czspig_product_critic`

## 设计原则

- 一期仍不实现完整登录，`users` 表只预留。
- Review 历史通过 `session_id` 做匿名隔离，继续保留 `X-Session-Id` 机制。
- `review_records` 保存用户输入、结构化报告 JSON、Markdown 报告、模型信息和状态。
- `ai_call_logs` 只保存 Prompt 哈希、脱敏摘要、模型名、状态和耗时，不保存真实 API Key 或完整系统提示词。
- 二期前不改 `review_records` 已有核心字段名，只补齐迭代、定位评分、失败原因和 Prompt 版本相关字段。

## 当前表结构

### users

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 用户 ID |
| username | VARCHAR(64) | 用户名，一期预留 |
| display_name | VARCHAR(64) | 展示名 |
| avatar_url | VARCHAR(512) | 头像地址 |
| status | VARCHAR(32) | 用户状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### review_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 评审记录 ID |
| user_id | BIGINT | 用户 ID，一期可为空 |
| session_id | VARCHAR(128) | 匿名会话 ID |
| input_content | MEDIUMTEXT | 用户原始输入 |
| input_summary | VARCHAR(255) | 输入摘要 |
| idea_group_id | VARCHAR(64) | 同一产品想法的迭代组；旧数据为空时后端按自身 `id` 兜底 |
| version_no | INT | 想法版本号，默认 1 |
| parent_review_id | BIGINT | 当前版本基于哪一次评审生成，第一版为空 |
| mode | VARCHAR(32) | `MENTOR` / `SHARP_PM` / `CLIENT` |
| roast_level | TINYINT | 1 温和，2 正常，3 毒舌 |
| one_line_verdict | VARCHAR(255) | 一句话评价 |
| beat_score | TINYINT | 毒打指数，0-100 |
| positioning_score | TINYINT | 产品定位评分，0-100 |
| report_json | JSON | 结构化报告，对应 `ReviewReportDto` |
| report_markdown | MEDIUMTEXT | Markdown 报告 |
| status | VARCHAR(32) | `PENDING` / `SUCCESS` / `FAILED` |
| error_message | VARCHAR(512) | 脱敏失败原因 |
| model_name | VARCHAR(128) | `deepseek-v4-flash` / `mock-product-reviewer-v2` / 其他配置模型 |
| prompt_version | VARCHAR(32) | 当前后端写入 `p2-review-quality-v1` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 软删除时间 |

索引：

- `idx_review_records_session_created(session_id, created_at)`
- `idx_review_records_group_version(idea_group_id, version_no)`
- `idx_review_records_parent_review_id(parent_review_id)`
- `idx_review_records_user_created(user_id, created_at)`
- `idx_review_records_status(status)`

### ai_call_logs

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
| input_tokens | INT | 输入 token，当前未接 usage 时为 0 |
| output_tokens | INT | 输出 token，当前未接 usage 时为 0 |
| latency_ms | INT | 调用耗时 |
| status | VARCHAR(32) | `SUCCESS` / `FAILED` |
| error_code | VARCHAR(128) | 错误码 |
| error_message | VARCHAR(512) | 脱敏错误信息 |
| created_at | DATETIME | 创建时间 |

## 初始化新库

```bash
mysql -u root -p < db/schema.sql
```

脚本默认使用 `utf8mb4_unicode_ci`，兼容 MySQL 5.7/8.0 的常见本地开发环境。

## 旧库升级

适用场景：本地或服务器已经用旧版 `schema.sql` 初始化过数据库，但缺少当前二期前代码需要的 `review_records` 字段或索引。

执行顺序：

1. 备份当前数据库。
2. 对照当前表结构确认缺失字段。
3. 执行迁移脚本中的缺失字段语句。
4. 执行索引语句；如果索引已存在，跳过对应语句。
5. 执行回填语句，把旧记录的 `idea_group_id` 回填为自身 `id`，并修正版本号。
6. 启动后端并创建一条新评审，确认历史列表和迭代详情可打开。

推荐命令：

```bash
mysqldump -u root -p czspig_product_critic > backup.sql
mysql -u root -p czspig_product_critic < db/migrations/20260630_review_records_phase2_upgrade.sql
```

注意事项：

- 迁移脚本没有重命名或删除 `review_records` 已有核心字段。
- 如果某个 `ADD COLUMN` 或 `CREATE INDEX` 提示已存在，说明该环境已经补过对应结构，跳过该语句继续执行剩余语句即可。
- `idea_group_id` 为空的旧数据，后端也会按记录自身 `id` 做轻量兜底；迁移脚本回填后，查询和迭代页面会更稳定。
- `positioning_score` 在旧库没有历史值时会默认补 0；旧历史报告 JSON 中如有真实评分，可后续单独写数据修复脚本回填。
- 迁移前后不要修改 `session_id`，否则会破坏历史记录隔离。
