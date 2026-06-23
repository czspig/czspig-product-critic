# 部署说明

本文档用于 MVP 上线前检查和手动部署。第一版不包含自动化部署脚本。

## 1. 构建

后端：

```bash
cd backend
mvn clean package
```

前端：

```bash
cd frontend
pnpm install
pnpm run build
```

前端产物目录：`frontend/dist`

## 2. 服务器环境

建议准备：

- JDK 17+
- MySQL 8.0
- Nginx
- Maven 或本地构建好的后端 jar
- Node.js 20+ 与 pnpm，仅构建机需要

## 3. 数据库

```bash
mysql -u root -p < db/schema.sql
```

生产库必须设置独立账号，不建议使用 root 连接应用。

## 4. 后端环境变量

```bash
export SERVER_PORT=8080
export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/czspig_product_critic?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export MYSQL_USERNAME='czspig_app'
export MYSQL_PASSWORD='replace-with-strong-password'
export APP_AI_PROVIDER='auto'
export APP_AI_FALLBACK_TO_MOCK='true'
export DEEPSEEK_API_KEY='replace-with-real-key'
export DEEPSEEK_BASE_URL='https://api.deepseek.com'
export DEEPSEEK_MODEL='deepseek-v4-flash'
```

启动：

```bash
java -jar backend/target/czspig-product-critic-backend-0.0.1-SNAPSHOT.jar
```

## 5. Nginx 示例

```nginx
server {
    listen 80;
    server_name example.com;

    root /var/www/czspig-product-critic/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 6. 上线前检查

- 确认 `DEEPSEEK_API_KEY` 只存在于服务器环境变量或密钥管理系统。
- 确认前端构建产物中没有真实 API Key。
- 确认 MySQL 用户只具备当前库所需权限。
- 确认 `APP_AI_FALLBACK_TO_MOCK` 的生产策略符合预期。
- 确认 Nginx 已限制上传体积，避免超大请求压垮后端。
- 确认后端日志不打印完整 Prompt、用户全文和 Provider 原始响应。
- 确认 `/api/health` 正常，前端可创建评审并查看历史。
