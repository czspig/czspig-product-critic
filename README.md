# czspig-product-critic

中文产品名：猪猪产品毒舌官

一个“犀利但鼓励”的 AI 产品经理评审 Agent。MVP 闭环是：输入产品想法，生成结构化评审报告，保存历史，查看详情，复制开发 Prompt，导出 Markdown。

## 当前能力

- 后端：Spring Boot 3、Java 17+、MyBatis Plus、MySQL
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router
- AI：DeepSeek Provider + Mock Provider 兜底
- 输出：一句话评价、毒打指数、定位评分、痛点分析、伪需求风险、功能冗余、冷启动问题、MVP 建议、最小可开发版本、开发 Prompt
- 历史：匿名 `X-Session-Id` 隔离历史记录
- 导出：前端支持复制完整报告、复制开发 Prompt、下载 Markdown

第一版不实现完整登录、支付、分享链接、复杂管理后台、多 Agent、RAG 和自动部署。

## 目录

```text
backend/   Spring Boot API
frontend/  Vue 3 Web App
db/        MySQL 初始化脚本
docs/      API、数据库、部署文档
```

## 初始化数据库

先确保本机 MySQL 可用，然后执行：

```bash
mysql -u root -p < db/schema.sql
```

默认数据库名：`czspig_product_critic`

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

可参考：[application-example.yml](backend/src/main/resources/application-example.yml)

Provider 选择规则：

- `APP_AI_PROVIDER=auto`：有 `DEEPSEEK_API_KEY` 时走 DeepSeek，否则走 Mock。
- `APP_AI_PROVIDER=deepseek`：强制走 DeepSeek；缺 key 或调用失败会按 `APP_AI_FALLBACK_TO_MOCK` 决定是否回退。
- `APP_AI_PROVIDER=mock`：只走本地 Mock。

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

当前机器如果没有 Maven，可以用 IntelliJ IDEA 导入 [pom.xml](backend/pom.xml) 后启动 `ProductCriticApplication`。

## 启动前端

```bash
cd frontend
pnpm install
pnpm run dev
```

默认访问：`http://127.0.0.1:5173`

前端开发服务器已配置 `/api` 代理到 `http://localhost:8080`。

构建：

```bash
cd frontend
pnpm run build
```

## 文档

- [API 文档](docs/api.md)
- [数据库设计](docs/database-schema.md)
- [部署说明](docs/deploy.md)
- [产品与架构说明](docs/product-architecture.md)

## 安全边界

- API Key 只通过后端环境变量读取，不进入前端代码和数据库。
- Prompt 日志只保存 SHA-256 哈希和脱敏摘要。
- 用户输入按 10-5000 字符校验。
- `mode` 和 `roastLevel` 做枚举与范围校验。
- DeepSeek 输出必须解析为结构化 JSON，失败会尝试一次修复，仍失败则按配置回退或记录失败。
- 异常响应不暴露 Java stack trace。
