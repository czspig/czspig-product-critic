# Codex 任务：完成项目工作台真实数据库验收与阶段 1 稳定化

## 一、当前项目上下文

- 项目名称：`czspig-product-critic`，中文名“猪猪产品毒舌官”。
- 项目类型：Vue 3 + Spring Boot 全栈 AI 产品评审应用。
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios。
- 后端：Spring Boot 3、Java 17、MyBatis Plus、MySQL。
- 当前阶段：二期阶段 1“项目工作台 MVP”代码已经实现，进入真实数据库验收与稳定化。
- 本轮任务类型：测试验证类 + 必要的最小 Bug 修复，不是新功能扩展。

上一轮已知结果：

- 后端 `./mvnw.cmd test` 已有 34 个测试通过。
- 前端 `pnpm build` 已通过类型检查和生产构建。
- 项目工作台已增加 `GET /api/projects/{ideaGroupId}`、`/projects/:groupId` 页面以及评审详情/版本页入口。
- 1440x1000 与 390x844 的固定响应数据页面检查通过。
- 固定测试响应能正确显示 V2 当前决策和 V1/V2 时间线。
- 真实 MySQL 联调尚未完成：本机 MySQL 拒绝默认 `root` 空密码并返回 `ERROR 1045`。
- 因此真实 V1 -> V2 创建、项目聚合接口、跨 session 隔离和完整导航链路尚未验收。
- 本机 `8080` 端口可能被 Codex 桌面应用占用；后端联调优先使用 `8081`。

## 二、本轮目标

请按“先验收、发现问题再最小修复”的顺序完成：

1. 使用有效的本地 MySQL 连接配置启动后端，且强制使用 Mock AI Provider。
2. 通过真实 HTTP 和真实 MySQL 跑通 V1 -> V2 -> 项目工作台完整数据链路。
3. 验证不同 `X-Session-Id` 不能读取同一项目。
4. 验证前端通过 `8081` 后端完成真实导航和数据展示。
5. 仅修复验收过程中确认存在的阶段 1 问题，并重新执行回归验证。
6. 形成一份可复用的阶段 1 验收记录。

本轮不要处理：

- 不进入阶段 2，不新增项目、假设、来源、证据或决策记录数据库表。
- 不接入联网搜索、行业报告、竞品数据或市场研究 Agent。
- 不实现登录、JWT、订阅、支付、聊天窗口、PPT 或 Word 生成。
- 不调整 DeepSeek Prompt、Provider、Parser 或评审质量规则，除非发现它们直接阻断 Mock 主流程；若发生，应先说明并等待确认。
- 不进行无关重构或全局视觉改版。
- 不自行提交 Git commit。

## 三、开始前必须检查

先读项目和现有改动，不要直接修改代码。至少检查：

1. `git status --short` 和 `git diff --stat`。
2. `README.md`。
3. `docs/phase2-development-plan.md`。
4. `docs/phase2-step-01-codex-prompt.md`。
5. `docs/api.md`、`docs/database-schema.md`、`docs/release-checklist.md`、`docs/deploy.md`。
6. `db/schema.sql` 和已有迁移脚本。
7. `backend/src/main/resources/application.yml` 与示例配置。
8. `ProjectController`、`ProjectWorkbenchResponse`、`ReviewService`、`ReviewServiceImpl` 和对应测试。
9. 前端 `vite.config.ts`、项目工作台页面、路由、API、store、类型和相关入口组件。
10. 项目现有启动进程和端口占用情况，不要终止与本任务无关的进程。

要求：

- 不要凭空假设路径或配置，以当前代码为准。
- 保留用户和上一轮 Codex 的所有未提交改动，不要回滚、覆盖或顺手格式化无关文件。
- 当前 AI Provider、Prompt、Parser 和相关测试存在未提交改动，本轮原则上不要修改。
- 如果本提示词与实际代码冲突，以实际代码为准，并说明差异。
- 开始操作前先简要汇报验收计划、需要的环境变量、预计使用的端口和可能修改的文件。

## 四、数据库凭据与安全边界

本轮需要有效的 MySQL 连接信息：

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

执行要求：

1. 只从当前进程环境变量或用户明确指定的本地未提交配置读取凭据。
2. 不要把真实密码写入 `application.yml`、`.env.example`、Markdown、测试代码或 Git 跟踪文件。
3. 不要在命令输出、日志或最终回复中回显真实密码和完整敏感连接串。
4. 不要读取 shell 历史、浏览器密码、系统凭据库或与本项目无关的配置来寻找密码。
5. 如果环境中没有有效凭据，停止启动和数据操作，明确告诉用户需要设置哪些变量，然后等待用户完成；不要猜密码，不要反复尝试常见密码。
6. 不得删除、重建或清空用户现有数据库。
7. 如有权限，优先使用独立验收数据库，例如 `czspig_product_critic_acceptance`；如果只能使用现有开发库，使用唯一 session ID 和明确的测试输入，并记录生成的 ID，不做直接 SQL 删除清理。
8. 初始化或迁移前先判断目标库是否为空、是否已有表和数据；任何可能影响现有数据的迁移都要先说明风险并等待确认。

## 五、具体任务

### 5.1 基线复验

在修改任何代码前先执行：

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
pnpm build
```

如果结果与上一轮不一致，先定位原因，不要继续叠加修改。

### 5.2 启动真实后端

使用：

- `SERVER_PORT=8081`
- `APP_AI_PROVIDER=mock`
- `APP_AI_FALLBACK_TO_MOCK=false`
- 用户提供的 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`

要求：

- 不修改默认 `application.yml` 来适配个人机器。
- 启动后先验证 `GET http://127.0.0.1:8081/api/health`。
- 确认后端确实连接 MySQL，不能用固定响应、内存替身或 Mock 数据库冒充真实联调。
- 不要把 Mock AI 结果表述为真实 DeepSeek 验收；本轮只验收业务数据链路。

### 5.3 创建真实 V1

使用唯一 session，例如：

```text
phase2-stage1-acceptance-a-<timestamp>
```

调用 `POST /api/reviews` 创建 V1。输入应包含目标用户、痛点、场景、差异化和商业模式，以确保 Mock 输出结构完整。

记录并校验：

- `id`
- `ideaGroupId`
- `versionNo = 1`
- `parentReviewId` 为空
- `groupVersionCount = 1`
- `status = SUCCESS`
- `providerName = mock`
- 报告包含当前项目工作台依赖的关键假设、未知问题、质量状态和验证计划；如果字段按当前 Mock 设计允许为空，要按真实契约判断，不得伪造。

### 5.4 基于 V1 创建真实 V2

使用同一个 `X-Session-Id`，提交明显更新后的产品想法，并正确传入 V1 的 `ideaGroupId` 和 `parentReviewId`。

校验：

- V2 `versionNo = 2`。
- V2 `parentReviewId` 等于 V1 ID。
- V2 `ideaGroupId` 与 V1 一致。
- `groupVersionCount = 2`。
- `GET /api/reviews/group/{ideaGroupId}` 按 V1、V2 顺序返回两条版本。
- V1 详情仍然可访问，且没有被 V2 覆盖。

### 5.5 验证项目工作台聚合

使用 session A 调用：

```http
GET /api/projects/{ideaGroupId}
X-Session-Id: <session-a>
```

校验至少包括：

- `projectId` 等于当前 `ideaGroupId`。
- `latestReviewId` 等于 V2 ID。
- `latestVersionNo = 2`。
- `totalVersions = 2`。
- 当前决策、决策原因、评分、质量状态、关键假设、未知问题、成功指标和验证计划来自 V2，而不是 V1。
- `versions` 同时包含 V1 和 V2，排序稳定。
- 响应中不存在其他 session 的数据。

不要只目测 JSON；使用结构化方式解析并对关键字段做明确断言。

### 5.6 验证跨 session 隔离

换用完全不同的 session B 请求同一项目：

```text
phase2-stage1-acceptance-b-<timestamp>
```

分别验证：

- `GET /api/projects/{ideaGroupId}` 不可访问。
- `GET /api/reviews/group/{ideaGroupId}` 不可访问。
- `GET /api/reviews/{v1Id}` 与 `{v2Id}` 不可访问。
- 错误 HTTP 状态和 `ApiResponse` 业务码符合现有全局异常约定。
- 错误响应不泄露目标项目是否属于其他用户之外的额外敏感信息。

### 5.7 验证前端真实链路

前端开发服务器应通过以下环境变量代理到 `8081`：

```text
VITE_API_PROXY_TARGET=http://127.0.0.1:8081
```

要求：

- 如果已有 5173 开发服务器指向错误后端，安全停止该项目自己的进程后再按正确代理目标启动；不要终止其他应用进程。
- 使用与创建 V1/V2 相同的 session 上下文访问前端。
- 从 V2 评审详情进入项目工作台。
- 确认项目工作台显示 V2 当前决策、关键假设、未知问题、验证计划和 V1/V2 时间线。
- 从项目工作台进入最新完整报告。
- 从项目工作台进入版本对比页，再返回项目工作台。
- 刷新 `/projects/:groupId` 后仍能从后端恢复数据。
- 使用 session B 访问项目路由时显示安全、可理解的错误状态，不显示 session A 数据。
- 检查浏览器控制台和网络请求，不得存在未处理异常或错误 API 地址。

至少检查以下视口：

- 1440x1000
- 390x844

确认没有横向溢出、文字截断、按钮重叠、时间线错位或加载状态引起的明显布局跳动。

### 5.8 最小修复规则

只有在上述验收确认失败时才修改代码，并遵守：

- 先复现、记录预期与实际结果，再分析根因。
- 优先修复项目工作台、版本聚合、session 隔离、前端代理或导航相关代码。
- 保持现有公开接口兼容，避免为测试方便改变业务语义。
- 不吞掉异常，不用固定数据掩盖数据库或接口问题。
- 不扩大到阶段 2 数据模型。
- 修改后重新执行受影响测试、完整后端测试、前端构建和失败的真实链路。

## 六、验收记录

新增或更新一份简洁的阶段 1 验收文档，建议路径：

```text
docs/phase2-stage1-acceptance.md
```

文档应记录：

- 验收日期和环境，不记录密码。
- 使用的端口、Provider 模式和数据库类型。
- V1/V2、项目工作台、跨 session、前端导航和响应式检查结果。
- 自动测试和构建结果。
- 未执行项及原因。
- 发现并修复的问题。
- 是否满足进入阶段 2 的条件。

不要把临时 session、测试记录 ID 或本地数据库名写成生产配置要求；它们只能作为本次验收说明。

## 七、完成门槛

只有同时满足以下条件，才能在总结中写“阶段 1 验收通过”：

1. 后端完整测试通过。
2. 前端生产构建通过。
3. 真实 MySQL 中成功创建 V1 和 V2。
4. 项目工作台真实聚合结果来自 V2，版本列表含 V1/V2。
5. session B 无法读取 session A 的项目和评审。
6. 前端通过真实接口完成主要导航链路。
7. 桌面和移动视口无阻断性布局问题。

如果缺少数据库凭据、环境不可用或任一关键项未执行，只能写“阶段 1 验收未完成”，并准确说明阻塞项；不要继续开发阶段 2。

## 八、最终输出要求

完成后请用中文输出：

1. 阶段 1 是否通过验收，给出明确结论。
2. 实际执行了哪些命令和验证步骤。
3. V1/V2、项目聚合和跨 session 的关键断言结果，不输出敏感凭据。
4. 前端导航、控制台和两个视口的验证结果。
5. 修改或新增了哪些文件，以及每个文件的目的。
6. 自动测试、构建和真实联调结果。
7. 未执行项、失败项及具体原因。
8. 仍然存在的风险和用户需要手动完成的事项。
9. 如果全部通过，只建议下一步进入“正式项目、假设、证据和决策数据模型设计”，不要在本轮直接实现。
