# czspig-product-critic

中文产品名：猪猪产品毒舌官

一个“犀利但鼓励”的 AI 产品经理评审 Agent。MVP 闭环是：输入产品想法，生成结构化评审报告，保存历史，查看详情，复制开发 Prompt，导出 Markdown。

第一版不实现完整登录、支付、分享链接、复杂管理后台、多 Agent、RAG 和自动部署。

## 技术栈

- 后端：Spring Boot 3、Java 17、MyBatis Plus、MySQL
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router
- AI：DeepSeek Provider + Mock Provider 兜底
- 构建：后端优先使用 Maven Wrapper，前端统一使用 pnpm

## 项目结构

```text
backend/   Spring Boot API
frontend/  Vue 3 Web App
db/        MySQL 初始化脚本
docs/      API、部署、评测和数据库文档
```

## 数据库初始化

先确保本机 MySQL 可用，然后执行：

```bash
mysql -u root -p < db/schema.sql
```

默认数据库名：`czspig_product_critic`。

## 后端配置

后端默认读取环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `MYSQL_URL` | 本机 `czspig_product_critic` | MySQL JDBC URL |
| `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | 空 | MySQL 密码 |
| `APP_ANONYMOUS_SESSION_ID` | `anonymous-dev-session` | 默认匿名 session |
| `APP_AI_PROVIDER` | `auto` | `auto` / `deepseek` / `mock` |
| `APP_AI_FALLBACK_TO_MOCK` | `true` | DeepSeek 失败时是否回退 Mock |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek OpenAI 兼容地址 |
| `DEEPSEEK_MODEL` | `deepseek-v4-flash` | 默认模型 |

可参考：[application-example.yml](backend/src/main/resources/application-example.yml)。不要把真实 API Key 或数据库密码提交到仓库。

Provider 选择规则：

- `APP_AI_PROVIDER=auto`：有 `DEEPSEEK_API_KEY` 时走 DeepSeek，否则走 Mock。
- `APP_AI_PROVIDER=deepseek`：强制走 DeepSeek；缺 key 或调用失败会按 `APP_AI_FALLBACK_TO_MOCK` 决定是否回退。
- `APP_AI_PROVIDER=mock`：只走本地 Mock。

## 启动后端

Linux / macOS：

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

后端构建：

```bash
cd backend
./mvnw -q -DskipTests package
```

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd -q -DskipTests package
```

## 启动前端

本项目统一使用 pnpm：

```bash
cd frontend
pnpm install
pnpm dev
```

默认访问：`http://127.0.0.1:5173`。

前端开发服务器已配置 `/api` 代理到 `http://localhost:8080`。

前端构建：

```bash
cd frontend
pnpm build
```

不推荐混用 npm/yarn。Windows PowerShell 如果遇到 `npm.ps1` 执行策略问题，可以使用 `npm.cmd run build`，但本项目仍建议用 pnpm。

## 文档

- [API 文档](docs/api.md)
- [数据库设计](docs/database-schema.md)
- [部署说明](docs/deploy.md)
- [产品与架构说明](docs/product-architecture.md)
- [人工评测说明](docs/eval.md)
- [上线前检查清单](docs/release-checklist.md)

## 常见问题

### 没有配置 DeepSeek API Key 会怎样？

默认 `APP_AI_PROVIDER=auto` 且 `APP_AI_FALLBACK_TO_MOCK=true`。没有 `DEEPSEEK_API_KEY` 时会使用 Mock Provider，便于本地开发和前端联调。

### API Key 放在哪里？

只放在服务器环境变量、本地未提交配置或部署平台密钥管理中。不要写入前端代码、Git 仓库、数据库或日志。

### 为什么优先使用 Maven Wrapper？

这样新机器不需要全局安装 Maven，也能通过 `./mvnw` 或 `.\mvnw.cmd` 构建后端。首次执行会下载 Maven 发行包。

## GitHub 提交流程

1. 运行前端构建：`cd frontend && pnpm build`
2. 运行后端构建：`cd backend && ./mvnw -q -DskipTests package`
3. 检查敏感信息：确认没有真实 API Key、数据库密码、`.env`、构建产物。
4. 查看变更：`git status`、`git diff`
5. 提交：`git add ... && git commit -m "prepare release checklist and deployment docs"`
6. 推送到 GitHub 后，按 [上线前检查清单](docs/release-checklist.md) 做手动验收。

## 阿里云部署前准备

- 准备 JDK 17、MySQL 8、Nginx、Node.js 20+、pnpm。
- 初始化数据库并创建最小权限 MySQL 用户。
- 在服务器环境变量中配置 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`DEEPSEEK_API_KEY`。
- 构建后端 jar 和前端 `dist`。
- 配置 Nginx：前端静态文件走 `/`，后端 API 反向代理到 `127.0.0.1:8080`。
- 放行安全组端口：`80/443`，仅在必要时临时开放 `8080`。
- 上线前执行 [上线前检查清单](docs/release-checklist.md)。

## 安全边界

- API Key 只通过后端环境变量读取，不进入前端代码和数据库。
- Prompt 日志只保存 SHA-256 哈希和脱敏摘要。
- 用户输入按 10-5000 字符校验。
- `mode` 和 `roastLevel` 做枚举与范围校验。
- DeepSeek 输出必须解析为结构化 JSON，失败会尝试一次修复，仍失败则按配置回退或记录失败。
- 异常响应不暴露 Java stack trace。
