# czspig-product-critic

中文产品名：猪猪产品毒舌官

一个“犀利但鼓励”的 AI 产品经理评审 Agent。当前闭环是：输入产品想法 -> 生成结构化评审报告 -> 保存历史 -> 查看详情 -> 复制开发 Prompt / 导出 Markdown，并支持同一个产品想法继续迭代为 V1/V2/V3。

当前能力：
- 输入增强助手：提示补充目标用户、痛点、使用场景、差异化和商业模式，并提供 3 个示例想法。
- 毒舌程度调节：温和 / 犀利 / 地狱三档，默认犀利模式。
- 多维评分展示：基于总分和报告内容展示用户痛点、市场空间、商业模式、实现难度、增长潜力、风险等级。
- 历史记录和版本迭代：基于 `X-Session-Id` 隔离匿名历史。
- AI Provider：支持 `auto` / `deepseek` / `mock`。

第一版不实现完整登录、支付、分享链路、复杂后台、多 Agent、RAG 或自动部署。

## 技术栈

- 后端：Spring Boot 3、Java 17、MyBatis Plus、MySQL
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router
- AI：DeepSeek Provider + Mock Provider
- 构建：后端优先使用 Maven Wrapper；前端可使用 pnpm，Windows 下也可用 `npm.cmd`

## 目录结构

```text
backend/   Spring Boot API
frontend/  Vue 3 Web App
db/        MySQL 初始化脚本
docs/      API、部署、评测和数据库文档
```

## 数据库初始化

确保 MySQL 可用后执行：

```bash
mysql -u root -p < db/schema.sql
```

默认数据库名：`czspig_product_critic`。

## 环境变量

后端实际读取的变量如下，示例见 [.env.example](.env.example) 和 [application-example.yml](backend/src/main/resources/application-example.yml)。

| 变量 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- |
| `SERVER_PORT` | `8080` | 否 | 后端端口 |
| `MYSQL_URL` | 本机 `czspig_product_critic` | 否 | MySQL JDBC URL |
| `MYSQL_USERNAME` | `root` | 否 | MySQL 用户名 |
| `MYSQL_PASSWORD` | 空 | 否 | MySQL 密码 |
| `APP_ANONYMOUS_SESSION_ID` | `anonymous-dev-session` | 否 | 未传 `X-Session-Id` 时的默认匿名会话 |
| `APP_AI_PROVIDER` | `auto` | 否 | `auto` / `deepseek` / `mock` |
| `APP_AI_FALLBACK_TO_MOCK` | `false` | 否 | DeepSeek 调用失败时是否回退 Mock |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek 模式必填 | DeepSeek API Key，只能放在后端环境变量或未提交配置 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | 否 | DeepSeek OpenAI 兼容地址 |
| `DEEPSEEK_MODEL` | `deepseek-v4-flash` | 否 | 模型名 |
| `DEEPSEEK_TIMEOUT_SECONDS` | `60` | 否 | 请求超时 |
| `DEEPSEEK_MAX_TOKENS` | `3000` | 否 | 输出 token 上限 |
| `DEEPSEEK_TEMPERATURE` | `0.6` | 否 | 生成温度 |

不要把真实 API Key、数据库密码或 `.env` 提交到 Git。

## AI Provider 策略

- `APP_AI_PROVIDER=mock`：只使用本地 Mock Provider。适合离线开发、前端联调和演示，前端会标记为“模拟评审”。
- `APP_AI_PROVIDER=deepseek`：强制使用 DeepSeek。未配置 `DEEPSEEK_API_KEY` 且 `APP_AI_FALLBACK_TO_MOCK=false` 时会报错；如果显式开启 fallback，则回退 Mock，并在响应中标记 `fallbackUsed=true`。
- `APP_AI_PROVIDER=auto`：有 `DEEPSEEK_API_KEY` 时优先 DeepSeek；没有 key 时使用 Mock，适合本地开发。Mock 结果不能当作真实 AI 验收。

真实 DeepSeek 输出会被解析为结构化 JSON；解析失败时会尝试一次 JSON 修复，仍失败则根据 fallback 配置回退或记录失败。

## 启动后端

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Linux / macOS：

```bash
cd backend
./mvnw spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

后端测试：

```bash
cd backend
./mvnw test
```

## 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

默认访问：`http://127.0.0.1:5173`。前端开发服务器已配置 `/api` 代理到 `http://localhost:8080`。

Windows PowerShell 如果遇到 `npm.ps1` 执行策略问题，可以使用：

```powershell
cd frontend
& 'C:\Program Files\nodejs\npm.cmd' run build
```

## 文档

- [API 文档](docs/api.md)
- [部署说明](docs/deploy.md)
- [数据库设计](docs/database-schema.md)
- [人工评测说明](docs/eval.md)
- [上线前检查清单](docs/release-checklist.md)

## 安全边界

- API Key 只通过后端环境变量读取，不进入前端代码、数据库或日志。
- 后端日志只记录 prompt hash 和脱敏摘要，不记录完整 Prompt、用户全文或 Authorization Header。
- 用户输入按 10-5000 字符校验。
- `mode` 和 `roastLevel` 做枚举与范围校验。
- `X-Session-Id` 用于历史记录隔离，不应破坏。
