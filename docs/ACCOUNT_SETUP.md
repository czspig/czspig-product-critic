# 账号、项目导航与个人中心

本轮仅本地实现和验证，没有部署上线。当前体验地址为 http://127.0.0.1:5175 ，API 在 8082。原端口占用，因此使用新端口。

## 实现范围

- 用户名注册与登录；用户名为 3–32 位 ASCII 字母、数字或下划线，服务端转小写。邮箱 trim 后转小写。数据库分别建立唯一索引，并捕获并发唯一性冲突。
- 昵称和邮箱必填；密码 10–72 字符且 UTF-8 不超过 72 字节，使用 Spring Security Crypto BCrypt（cost 12）。未验证账号只能处于待验证状态，不能登录。
- 注册验证、重发、找回密码、单次密码重置链接；32 字节安全随机凭据，数据库仅存 SHA-256 摘要，30 分钟有效，重发使原同用途凭据失效，消费使用行锁。
- HttpOnly、SameSite=Lax 容器会话，登录旋转会话；生产强制 Secure Cookie。会话空闲 2 小时失效。密码修改/重置递增账号会话版本，使所有旧会话失效；修改要求当前密码。
- CSRF 会话随机值和请求头校验，包含登录/退出/匿名评审等所有写操作。CORS 仅允许可信配置的产品 Origin，支持 Cookie。前端不在 localStorage 保存认证凭据。
- 匿名身份改为服务端签发的 HttpOnly 随机 Cookie，数据库保留其摘要。所有评审/项目接口使用服务端解析的归属键，忽略 `X-Session-Id` 和客户端用户编号。新账号记录同时写入现有 `review_records.user_id`。
- 项目列表服务端搜索、每页 20 项、最近活动排序，桌面收起/移动抽屉、当前项目高亮、重命名、整项目软删除。历史作为次要入口；版本仍在项目内。
- 个人中心仅昵称、固定用户名、注册绑定邮箱及验证状态、修改密码和退出。头像使用昵称首字。
- 草稿按账号/匿名身份、项目、来源报告隔离；显式迁移整个项目、全部版本及对应本地草稿，独立未提交匿名评审草稿不混入账号。迁移收据支持本地保存失败后的重试；保存读回确认后才删除原草稿。
- 退出清除当前账号草稿和应用状态；跨标签页通过存储事件同步身份变化，焦点检查和 `pageshow` 刷新保护缓存页面。旧身份的迟到请求结果会被丢弃。CSRF 失效仅在身份未变化时重试一次。

## 数据库迁移

已有数据库**不要重新执行初始化 `db/schema.sql`**。启动后 `AccountSchema` 自动执行以下仅新增、可重复的脚本：

1. `backend/src/main/resources/db/accounts-v1.sql`
2. `backend/src/main/resources/db/accounts-v2.sql`

记录在 `account_schema_versions` 中，可查询版本及首次应用时间。创建 `accounts`、`account_tokens`、`anonymous_owners`、`project_names`、`account_rate_limits`、`anonymous_project_migrations`；不重建或清空已有表。已有 `users` 表保持原样，本轮认证使用独立 `accounts`，不会自动将旧占位用户转为已验证账号。没有新增表间硬删除级联；项目删除设置 `deleted_at`，保留原报告和 AI 日志。

新库才需要先执行原初始化 SQL。对已有环境，迁移账号应有上述表的 CREATE/INSERT 权限；若使用独立数据库迁移身份，可按顺序预执行脚本。后续升级应新增版本脚本，不修改已发布脚本。先备份数据库，再执行升级。

**旧匿名数据兼容限制：**升级前 `X-Session-Id` 由客户端自行提供，没有服务端签名或所有权证明。因此旧记录和旧本地草稿保留，**不会自动展示或认领到新的匿名 Cookie/账号**。不能把“知道旧 Session 或项目编号”当作证明；这部分历史需要管理员在确认真实归属后单独迁移。此项未实现自动恢复流程，是上线前需要决定的历史数据处理方式。

## 本地启动

PowerShell，项目根目录（真实数据库密码只在当前终端环境设置）：

```powershell
$env:MYSQL_USERNAME='root'
$env:MYSQL_PASSWORD='<本地数据库密码>'
$env:SERVER_PORT='8082'
$env:SPRING_PROFILES_ACTIVE='dev'
$env:APP_AI_PROVIDER='mock'
$env:APP_MAIL_MODE='capture'
$env:APP_PUBLIC_ORIGIN='http://127.0.0.1:5175'
$env:APP_AUTH_SECURE_COOKIE='false'
backend/mvnw.cmd -f backend/pom.xml spring-boot:run
```

另一个终端：

```powershell
cd frontend
$env:VITE_API_PROXY_TARGET='http://127.0.0.1:8082'
npm.cmd run dev -- --host 127.0.0.1 --port 5175 --configLoader runner
```

端口改变时，前端端口、代理目标和 `APP_PUBLIC_ORIGIN` 必须一致。邮件链接和 CORS 的可信地址都来自该配置，不接受请求里的 Host/return URL 生成邮件地址。

开发邮件捕获**不会发送真实邮件**，只在包含 `dev` 且不包含 `prod` 的配置下启用；其他情况下尝试启用捕获会拒绝启动。开发捕获接口仅接受本机来源：

```text
GET http://127.0.0.1:8082/api/account/dev-mail?email=<独立测试邮箱>
```

返回明确标记为开发邮件捕获的 link，打开后仍需人工点击验证/提交密码，不会自动完成验证。捕获在内存中，重启后丢失；可重新申请邮件。不要公开开发捕获环境。

## 正式环境配置示例（尚未部署）

```dotenv
SPRING_PROFILES_ACTIVE=prod
APP_MAIL_MODE=smtp
APP_PUBLIC_ORIGIN=https://your-product.example.com
APP_AUTH_SECURE_COOKIE=true
MAIL_HOST=smtp.your-provider.example
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=<通过部署平台注入，勿提交>
MAIL_FROM=no-reply@your-product.example.com
APP_ANONYMOUS_QUOTA=3
APP_DAILY_REVIEW_LIMIT=200
APP_AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=<通过部署平台注入，勿提交>
MYSQL_PASSWORD=<通过部署平台注入，勿提交>
```

SMTP 使用 STARTTLS，并设置连接/读写超时。需要补充 SMTP 服务、授权发件人及域名 SPF/DKIM/DMARC 配置、可信 HTTPS 产品域名、同源 `/api` 反向代理、真实 AI Key。反向代理访问日志不要记录邮箱验证/重置页面的 query 参数；前端已设置 no-referrer，API 响应 no-store。前端入口 HTML 也应设置 no-store 或合理的重新验证策略。

未配置 SMTP Host、发件地址或发送器时，注册/重发/找回返回 HTTP 500 和“邮件服务尚未配置，请联系管理员”；发送异常返回“邮件发送失败，请稍后重试”。不存在邮箱的找回返回统一条件式提示，以避免枚举账号。没有自动验证或假“邮件已发送”。

本轮采用单实例 Servlet 会话，服务重启使登录失效；多实例需要共享会话或明确粘性会话策略，这一部署能力尚未验证。不要开启请求体/SMTP 调试日志记录密码或邮件凭据。

## 额度和限流口径

**匿名成功额度 3 次是暂定配置，不代表正式上线政策。**`APP_ANONYMOUS_QUOTA` 可调整：V1 和每次迭代的成功报告各计 1 次，失败不计成功额度。额度保存在服务端匿名凭据记录中，删除项目或把项目迁移到账号不重置额度。

基础防护：API 每 IP 300 次/分钟；登录每 IP/用户名 12 次/15 分钟；注册 5 次/15 分钟；邮件申请 5 次/15 分钟；验证/重置凭据每 IP/凭据 10 次/15 分钟；评审每 IP 20 次/小时；全部身份合计默认 200 次评审尝试/日（可用 `APP_DAILY_REVIEW_LIMIT` 调整）。每日保护记录尝试，包含失败或被拦截的尝试，区别于匿名成功额度。

限流计数存数据库，可跨进程保持；IP 采用直接连接来源，不盲信客户端转发头。代理环境应再配边缘 IP 限流，否则多个用户会共享代理 IP 的限制。清 Cookie、换浏览器/IP 等仍能绕过部分匿名限制；**不能完全阻止成本绕过**，以上仅基础保护。需要按实际流量调整预算并清理过期限流桶和已过期凭据。

## 验证

完整验收和截图见 [account-verification/README.md](account-verification/README.md)。AI 使用 Mock，邮件使用开发捕获；真实 SMTP 投递、真实 AI、公网 HTTPS 和多实例没有实测。

```powershell
# 使用测试数据库最佳；测试会创建独立 example.test 账号及项目。
$env:MYSQL_PASSWORD='<数据库密码>'
backend/mvnw.cmd -f backend/pom.xml test
cd frontend
npm.cmd run build
node --test tests/account.test.cjs tests/experience.test.cjs
```

本次后端 39 项测试通过，前端 6 项通过，生产构建通过。测试仅软删除自身生成的分页夹具、版本测试项目和浏览器明确标记为“可删除测试”的项目；保留既有数据。未验证真实邮箱收件、正式域名 Cookie/TLS、分布式会话、限流高负载和浏览器最终下载文件落盘。
