# API 文档

所有接口均由 Spring Boot 后端提供，统一返回 `ApiResponse<T>`。前端通过 Vite 代理访问 `/api`。

## 统一响应

成功：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "请求参数不合法",
  "data": null
}
```

## 匿名 session

第一版不实现完整登录。所有 Review 接口支持请求头：

```http
X-Session-Id: local-test-session
```

不传时使用默认 `anonymous-dev-session`。历史记录和详情查询按 `session_id` 隔离。

## AI Provider

后端支持 `auto` / `deepseek` / `mock` 三种 Provider 模式。真实 DeepSeek 输出必须解析为结构化 JSON；解析失败会尝试一次 JSON 修复，仍失败则按 `APP_AI_FALLBACK_TO_MOCK` 配置回退 Mock 或记录失败。

## 1. 健康检查

`GET /api/health`

响应示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "status": "UP",
    "service": "czspig-product-critic",
    "time": "2026-06-23 12:00:00"
  }
}
```

## 2. 创建评审

`POST /api/reviews`

请求：

```json
{
  "content": "我想做一个帮助大学生找搭子的校园小程序",
  "mode": "SHARP_PM",
  "roastLevel": 2
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| content | string | 是 | 产品想法或需求内容，10-5000 字符 |
| mode | string | 是 | `MENTOR`、`SHARP_PM`、`CLIENT` |
| roastLevel | number | 是 | 1 温和，2 正常，3 毒舌 |

响应关键字段：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "id": 1,
    "inputSummary": "我想做一个帮助大学生找搭子的校园小程序",
    "mode": "SHARP_PM",
    "roastLevel": 2,
    "oneLineVerdict": "这个想法有场景，但现在还像功能清单，不像一个真正的产品切口。",
    "beatScore": 72,
    "positioningScore": 68,
    "report": {},
    "reportMarkdown": "...",
    "status": "SUCCESS",
    "errorMessage": null,
    "createdAt": "2026-06-23 12:00:00"
  }
}
```

## 3. 获取历史记录

`GET /api/reviews?page=1&pageSize=10`

响应字段：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "page": 1,
    "pageSize": 10,
    "total": 1,
    "items": [
      {
        "id": 1,
        "inputSummary": "我想做一个帮助大学生找搭子的校园小程序",
        "mode": "SHARP_PM",
        "roastLevel": 2,
        "oneLineVerdict": "这个想法有场景，但现在还像功能清单，不像一个真正的产品切口。",
        "beatScore": 72,
        "positioningScore": 68,
        "status": "SUCCESS",
        "errorMessage": null,
        "createdAt": "2026-06-23 12:00:00"
      }
    ]
  }
}
```

## 4. 获取评审详情

`GET /api/reviews/{id}`

返回完整字段：

- 用户原始输入
- 输入摘要
- 评审模式
- 吐槽强度
- 一句话评价
- 毒打指数
- 产品定位评分
- 结构化报告 `report`
- JSON 字符串 `reportJson`
- Markdown 报告 `reportMarkdown`
- 状态
- 脱敏失败原因 `errorMessage`
- 创建时间

## 5. curl 验证示例

健康检查：

```bash
curl http://localhost:8080/api/health
```

创建评审：

```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: local-test-session" \
  -d '{"content":"我想做一个帮助大学生找搭子的校园小程序，先从同校兴趣匹配和活动邀约开始。","mode":"SHARP_PM","roastLevel":2}'
```

获取历史：

```bash
curl -H "X-Session-Id: local-test-session" "http://localhost:8080/api/reviews?page=1&pageSize=10"
```

获取详情：

```bash
curl -H "X-Session-Id: local-test-session" http://localhost:8080/api/reviews/1
```
