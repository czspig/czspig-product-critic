# 部署说明

本文档用于 MVP 上线前检查和手动部署。第一版不包含自动化部署脚本。

## 1. 服务器准备

建议配置：

- JDK 17+
- MySQL 8.0+
- Nginx 1.20+
- Node.js 20+
- pnpm 或 npm

安全组建议：

| 端口 | 用途 | 建议 |
| --- | --- | --- |
| `80` | HTTP | 开放 |
| `443` | HTTPS | 配好证书后开放 |
| `22` | SSH | 仅允许可信 IP |
| `8080` | Spring Boot | 不直接暴露公网，交给 Nginx 反代 |
| `3306` | MySQL | 不开放公网 |

## 2. 数据库初始化

生产环境建议创建最小权限用户：

```sql
CREATE DATABASE IF NOT EXISTS czspig_product_critic
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER 'czspig_app'@'127.0.0.1' IDENTIFIED BY 'replace-with-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON czspig_product_critic.* TO 'czspig_app'@'127.0.0.1';
FLUSH PRIVILEGES;
```

初始化表结构：

```bash
mysql -u root -p < db/schema.sql
```

## 3. 后端环境变量

生产环境不要把真实密钥写进仓库。推荐写入 systemd 环境变量、部署平台密钥管理或服务器本地未提交配置。

```bash
export SERVER_PORT=8080
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/czspig_product_critic?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export MYSQL_USERNAME='czspig_app'
export MYSQL_PASSWORD='replace-with-strong-password'
export APP_ANONYMOUS_SESSION_ID='anonymous-prod-session'
export APP_AI_PROVIDER='deepseek'
export APP_AI_FALLBACK_TO_MOCK='false'
export DEEPSEEK_API_KEY='replace-with-real-key'
export DEEPSEEK_BASE_URL='https://api.deepseek.com'
export DEEPSEEK_MODEL='deepseek-v4-flash'
```

Provider 建议：

- 本地演示：`APP_AI_PROVIDER=mock`
- 本地自动：`APP_AI_PROVIDER=auto`，没有 key 时会走 Mock
- 正式验收：`APP_AI_PROVIDER=deepseek` 且 `APP_AI_FALLBACK_TO_MOCK=false`
- 临时降级：明确告知用户后再设置 `APP_AI_PROVIDER=mock` 或 `APP_AI_FALLBACK_TO_MOCK=true`

## 4. 构建

后端：

```bash
cd backend
./mvnw -q -DskipTests package
```

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd -q -DskipTests package
```

前端：

```bash
cd frontend
pnpm install
pnpm build
```

Windows PowerShell 如果遇到脚本策略问题，可用：

```powershell
cd frontend
& 'C:\Program Files\nodejs\npm.cmd' run build
```

## 5. systemd 示例

```ini
[Unit]
Description=czspig-product-critic backend
After=network.target mysql.service

[Service]
WorkingDirectory=/opt/czspig-product-critic
ExecStart=/usr/bin/java -jar /opt/czspig-product-critic/backend/target/czspig-product-critic-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5
Environment=SERVER_PORT=8080
Environment=MYSQL_URL=jdbc:mysql://127.0.0.1:3306/czspig_product_critic?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
Environment=MYSQL_USERNAME=czspig_app
Environment=MYSQL_PASSWORD=replace-with-strong-password
Environment=APP_AI_PROVIDER=deepseek
Environment=APP_AI_FALLBACK_TO_MOCK=false
Environment=DEEPSEEK_API_KEY=replace-with-real-key
Environment=DEEPSEEK_BASE_URL=https://api.deepseek.com
Environment=DEEPSEEK_MODEL=deepseek-v4-flash

[Install]
WantedBy=multi-user.target
```

真实密钥建议放到受限权限的 EnvironmentFile 或密钥管理系统中，不要提交到 GitHub。

## 6. Nginx 反向代理

```nginx
server {
    listen 80;
    server_name example.com;

    root /var/www/czspig-product-critic/dist;
    index index.html;

    client_max_body_size 1m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 130s;
    }
}
```

## 7. 上线前安全检查

- `DEEPSEEK_API_KEY` 只存在于服务器环境变量或密钥管理系统。
- 前端构建产物不包含真实 API Key。
- MySQL 用户只具备当前库所需权限。
- `.env`、`application-local.yml`、`application-dev.yml` 不进入 Git。
- `target/`、`dist/`、`node_modules/` 不进入 Git。
- 后端日志不打印完整 Prompt、用户全文、Provider 原始响应或真实密钥。
- 生产环境 fallback 策略符合预期，避免用户误以为 Mock 是真实 AI。

## 8. 验收步骤

1. `GET /api/health` 正常。
2. 前端首页可打开。
3. 示例填充可用。
4. 选择温和 / 犀利 / 地狱模式后可生成报告。
5. 详情页展示实际 Provider、模型名和 fallback 状态。
6. 历史记录能进入同一条详情。
7. 版本迭代和版本对比可用。
8. 复制开发 Prompt、复制完整报告、导出 Markdown 可用。
9. 配置真实 `DEEPSEEK_API_KEY` 后，确认 `providerName=deepseek`。

## 9. 回滚

- 代码回滚：切回上一版 jar 和前端 dist。
- 配置回滚：恢复上一版 systemd 环境变量或密钥配置。
- AI 降级：临时设置 `APP_AI_PROVIDER=mock`，并在 UI 或交付说明中标明模拟评审。
- 数据回滚：上线前备份 MySQL，必要时按备份恢复。
