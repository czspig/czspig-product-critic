# 部署说明

本文档用于 MVP 上线前检查和阿里云手动部署。第一版不包含自动化部署脚本。

## 1. 阿里云服务器准备

建议配置：

- 操作系统：Alibaba Cloud Linux、Ubuntu 22.04 LTS 或同类 Linux
- JDK：17+
- MySQL：8.0+
- Nginx：1.20+
- Node.js：20+
- pnpm：9+

安全组建议：

| 端口 | 用途 | 建议 |
| --- | --- | --- |
| `80` | HTTP | 开放 |
| `443` | HTTPS | 配好证书后开放 |
| `22` | SSH | 仅允许可信 IP |
| `8080` | Spring Boot | 不直接暴露公网，交给 Nginx 反代 |
| `3306` | MySQL | 不开放公网 |

## 2. 数据库初始化

在服务器上创建数据库和最小权限用户：

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
export APP_AI_PROVIDER='auto'
export APP_AI_FALLBACK_TO_MOCK='true'
export DEEPSEEK_API_KEY='replace-with-real-key'
export DEEPSEEK_BASE_URL='https://api.deepseek.com'
export DEEPSEEK_MODEL='deepseek-v4-flash'
```

`APP_AI_PROVIDER=auto` 时，没有 `DEEPSEEK_API_KEY` 会走 Mock。生产环境如果必须真实 AI 输出，请配置 key 并完成人工验收。

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

后端产物：

```text
backend/target/czspig-product-critic-backend-0.0.1-SNAPSHOT.jar
```

前端：

```bash
cd frontend
pnpm install
pnpm build
```

前端产物目录：

```text
frontend/dist
```

## 5. 后台运行方式

临时验证：

```bash
java -jar backend/target/czspig-product-critic-backend-0.0.1-SNAPSHOT.jar
```

生产建议使用 systemd。示例：

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
Environment=APP_AI_PROVIDER=auto
Environment=APP_AI_FALLBACK_TO_MOCK=true
Environment=DEEPSEEK_API_KEY=replace-with-real-key

[Install]
WantedBy=multi-user.target
```

把真实密钥放在受限权限文件中会更安全，不要把真实值提交到 GitHub。

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

后续配置 HTTPS 时，建议使用 Certbot 或阿里云证书服务。

## 7. 上线前安全检查

- `DEEPSEEK_API_KEY` 只存在于服务器环境变量或密钥管理系统。
- 前端构建产物中没有真实 API Key。
- MySQL 用户只具备当前库所需权限。
- `.env`、`application-local.yml`、`application-dev.yml` 不进入 Git。
- `target/`、`dist/`、`node_modules/` 不进入 Git。
- 后端日志不打印完整 Prompt、用户全文、Provider 原始响应或真实密钥。
- `APP_AI_FALLBACK_TO_MOCK` 的生产策略符合预期。
- Nginx 限制请求体大小，避免超大请求压垮后端。

## 8. 验收步骤

1. `GET /api/health` 正常。
2. 前端首页可打开。
3. 示例填充可用。
4. 开始评审后可生成报告。
5. 首页右侧摘要展示决策、分数、成功指标和验证动作。
6. 详情页可查看完整报告。
7. 历史记录能进入同一条详情。
8. 复制开发 Prompt、复制完整报告、导出 Markdown 可用。
9. 用 [人工评测说明](eval.md) 的 3 个样例检查 Agent 输出质量。

## 9. 回滚

- 代码回滚：切回上一版 jar 和前端 dist。
- 配置回滚：恢复上一版 systemd 环境变量或密钥配置。
- AI 降级：临时设置 `APP_AI_PROVIDER=mock` 或 `APP_AI_FALLBACK_TO_MOCK=true`。
- 数据回滚：上线前备份 MySQL，必要时按备份恢复。
