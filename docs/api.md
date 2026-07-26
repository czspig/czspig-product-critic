# API 文档

所有接口由 Spring Boot 后端提供，统一返回 `ApiResponse<T>`。前端开发环境通过 Vite 代理访问 `/api`。

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
  "code": "AI_PROVIDER_ERROR",
  "message": "DeepSeek 调用触发限流，请稍后再试或检查额度",
  "data": null
}
```

常见错误码：

| code | 说明 |
| --- | --- |
| `VALIDATION_ERROR` | 请求参数不合法、JSON 请求体格式错误 |
| `AI_PROVIDER_ERROR` | DeepSeek / Mock / 输出解析相关错误 |
| `DATABASE_ERROR` | 数据库存储或查询失败 |
| `NOT_FOUND` | 记录不存在或不属于当前 session |
| `INTERNAL_ERROR` | 未预期服务错误 |

## 匿名 Session

第一版不实现完整登录。所有 Review 接口支持请求头：

```http
X-Session-Id: local-test-session
```

不传时使用 `APP_ANONYMOUS_SESSION_ID`，默认 `anonymous-dev-session`。历史记录和详情查询按 `session_id` 隔离。

## AI Provider 字段

创建评审和详情接口会返回：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `providerName` | string | 实际执行 Provider：`deepseek` / `mock` / `unknown` |
| `modelName` | string | 实际模型名，如 `deepseek-v4-flash` / `mock-product-reviewer-v2` |
| `fallbackUsed` | boolean | 是否从 DeepSeek 回退到了 Mock |

Provider 策略：

- `APP_AI_PROVIDER=mock`：只走 Mock，适合本地开发和演示。
- `APP_AI_PROVIDER=deepseek`：强制 DeepSeek。缺 key 且 fallback 关闭时返回错误；fallback 开启时可回退 Mock。
- `APP_AI_PROVIDER=auto`：有 `DEEPSEEK_API_KEY` 时走 DeepSeek；没有 key 时走 Mock。

Mock 结果只代表“模拟评审”，不能当作真实 AI 验收。

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
  "content": "我想做一个校园二手交易小程序，目标用户是同校学生，主要解决毕业季教材和宿舍用品转卖的问题。",
  "mode": "SHARP_PM",
  "roastLevel": 2,
  "ideaGroupId": "optional",
  "parentReviewId": 12
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `content` | string | 是 | 产品想法或需求内容，10-5000 字符 |
| `mode` | string | 是 | `MENTOR` / `SHARP_PM` / `CLIENT` |
| `roastLevel` | number | 否 | 1 温和，2 犀利，3 地狱；旧请求不传时后端按 2 处理 |
| `ideaGroupId` | string | 否 | 同一产品想法的迭代组，首次提交可不传 |
| `parentReviewId` | number | 否 | 基于哪次评审继续优化，提交 V2/V3 时传 |

响应关键字段：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "id": 1,
    "inputContent": "我想做一个校园二手交易小程序...",
    "inputSummary": "我想做一个校园二手交易小程序...",
    "ideaGroupId": "1",
    "versionNo": 1,
    "parentReviewId": null,
    "groupVersionCount": 1,
    "mode": "SHARP_PM",
    "roastLevel": 2,
    "oneLineVerdict": "犀利模式先收拾一下：校园二手交易不缺摊位...",
    "beatScore": 72,
    "positioningScore": 68,
    "report": {
      "oneLineVerdict": "...",
      "reviewTargetType": "NEW_IDEA",
      "goDecision": "CONTINUE",
      "goDecisionReason": "...",
      "beatScore": 72,
      "positioningScore": 68,
      "painPointAnalysis": "...",
      "fakeDemandRisks": ["..."],
      "featureRedundancyCheck": ["..."],
      "coldStartProblems": ["..."],
      "mvpSuggestions": ["..."],
      "minimumBuildVersion": {
        "goal": "...",
        "coreFeatures": ["..."],
        "excludedFeatures": ["..."],
        "successMetric": "...",
        "validationPlan": ["..."]
      },
      "developerPrompt": "..."
    },
    "reportJson": "{...}",
    "reportMarkdown": "...",
    "status": "SUCCESS",
    "errorMessage": null,
    "providerName": "mock",
    "modelName": "mock-product-reviewer-v2",
    "fallbackUsed": false,
    "createdAt": "2026-06-23 12:00:00"
  }
}
```

版本规则：

- 首次提交不传 `ideaGroupId` / `parentReviewId`，后端生成 V1，并用当前评审 `id` 作为 `ideaGroupId`。
- 基于旧评审继续优化时传 `parentReviewId`，后端沿用父级 `ideaGroupId`，版本号自动加一。
- 旧数据没有 `ideaGroupId` 时，后端按该记录自身 `id` 兜底。

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
        "inputSummary": "我想做一个校园二手交易小程序...",
        "ideaGroupId": "1",
        "versionNo": 1,
        "parentReviewId": null,
        "groupVersionCount": 1,
        "mode": "SHARP_PM",
        "roastLevel": 2,
        "oneLineVerdict": "...",
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

历史列表暂不返回 `providerName` / `fallbackUsed`，详情页会返回。

## 4. 获取评审详情

`GET /api/reviews/{id}`

返回完整字段：

- 用户原始输入 `inputContent`
- 输入摘要 `inputSummary`
- 想法迭代组 `ideaGroupId`
- 当前版本号 `versionNo`
- 父级评审 `parentReviewId`
- 同组版本数 `groupVersionCount`
- 评审模式 `mode`
- 毒舌强度 `roastLevel`
- 一句话评价 `oneLineVerdict`
- 毒打指数 `beatScore`
- 产品定位评分 `positioningScore`
- 结构化报告 `report`
- JSON 字符串 `reportJson`
- Markdown 报告 `reportMarkdown`
- 状态 `status`
- 脱敏失败原因 `errorMessage`
- 实际 Provider `providerName`
- 模型名 `modelName`
- 是否 fallback `fallbackUsed`
- 创建时间 `createdAt`

## 5. 获取同组迭代版本

`GET /api/reviews/group/{ideaGroupId}`

按匿名 session 隔离，只返回同一想法组下的版本，按 `versionNo` 升序。

响应示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "ideaGroupId": "1",
    "versions": [
      {
        "id": 1,
        "versionNo": 1,
        "parentReviewId": null,
        "goDecision": "PIVOT",
        "goDecisionReason": "当前切入点需要收缩。",
        "beatScore": 78,
        "positioningScore": 55,
        "oneLineVerdict": "...",
        "successMetric": "...",
        "minimumBuildGoal": "...",
        "coreFeatures": ["..."],
        "excludedFeatures": ["..."],
        "validationPlan": ["..."],
        "createdAt": "2026-06-23 12:00:00"
      }
    ]
  }
}
```

## 6. 获取项目工作台

`GET /api/projects/{ideaGroupId}`

请求头：

```http
X-Session-Id: local-test-session
```

当前阶段暂时由 `ideaGroupId` 承担项目标识。接口只聚合当前 session 下同组的评审记录，不返回原始 `reportJson`；后续引入正式项目实体时，该路径参数会迁移为正式项目 ID，并保留兼容边界。

响应示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "projectId": "1",
    "title": "校园教材交易工具，先验证毕业季同校供需匹配",
    "latestReviewId": 12,
    "latestVersionNo": 2,
    "updatedAt": "2026-07-23 10:30:00",
    "totalVersions": 2,
    "decision": "PIVOT",
    "decisionReason": "先收缩到教材品类和单一校区。",
    "oneLineVerdict": "方向可验证，但综合交易平台范围过大。",
    "beatScore": 58,
    "positioningScore": 72,
    "overallScore": 68,
    "qualityStatus": "PASS",
    "qualitySummary": "关键结论与验证动作完整。",
    "providedFacts": [
      {
        "statement": "毕业季存在集中转卖需求",
        "sourceText": "用户输入"
      }
    ],
    "inferences": [],
    "criticalHypotheses": [
      {
        "statement": "同校学生愿意线下完成交易",
        "importance": "HIGH",
        "impactIfFalse": "供需匹配不能形成闭环",
        "relatedDimensions": ["user_value"]
      }
    ],
    "unknownQuestions": [
      {
        "question": "一周内能否形成足够的有效供需",
        "reason": "缺少真实交易数据"
      }
    ],
    "successMetric": "一周内完成 10 次有效联系",
    "validationPlan": ["访谈 5 名毕业生", "发布教材求购表单"],
    "versions": [
      {
        "id": 11,
        "versionNo": 1,
        "parentReviewId": null,
        "goDecision": "CONTINUE",
        "goDecisionReason": "先验证需求。",
        "beatScore": 70,
        "positioningScore": 60,
        "oneLineVerdict": "需求存在，但范围需要收缩。",
        "successMetric": "完成 5 次访谈",
        "minimumBuildGoal": "验证交易意愿",
        "coreFeatures": ["发布教材", "联系卖家"],
        "excludedFeatures": ["支付"],
        "validationPlan": ["访谈目标用户"],
        "scoreChangeReason": null,
        "confidenceSummary": "高 1 / 中 3 / 低 2",
        "criticalHypotheses": ["同校学生愿意线下完成交易"],
        "qualityStatus": "WARNING",
        "createdAt": "2026-07-22 10:00:00"
      }
    ]
  }
}
```

聚合规则：

- 版本先按 `versionNo`，再按更新时间、创建时间和记录 ID 明确排序；最高版本作为当前决策来源。
- 标题取当前版本的 `inputSummary`，决策、知识边界、质量状态和验证计划取当前版本的结构化报告。
- 可选知识边界和验证列表缺失时返回空数组，不返回 `null`。
- 版本时间线复用同组迭代版本结构并按版本号升序返回。

隔离与错误语义：

- `X-Session-Id` 的默认值和校验规则与 Review 接口一致。
- 项目不存在或属于其他 session 时统一返回 HTTP `404` 和业务码 `NOT_FOUND`，不会暴露其他 session 是否持有该项目。
- 旧记录的 `ideaGroupId` 为空时，继续沿用“记录自身 ID 作为组 ID”的兼容规则。

## 7. curl 验证示例

健康检查：

```bash
curl http://localhost:8080/api/health
```

Mock 模式创建评审：

```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: local-test-session" \
  -d '{"content":"校园二手交易小程序，目标用户是同校学生，解决毕业季教材和宿舍用品转卖问题，先从一个校区和教材品类做起。","mode":"SHARP_PM","roastLevel":2}'
```

获取历史：

```bash
curl -H "X-Session-Id: local-test-session" "http://localhost:8080/api/reviews?page=1&pageSize=10"
```

获取详情：

```bash
curl -H "X-Session-Id: local-test-session" http://localhost:8080/api/reviews/1
```

获取同组版本：

```bash
curl -H "X-Session-Id: local-test-session" http://localhost:8080/api/reviews/group/1
```

获取项目工作台：

```bash
curl -H "X-Session-Id: local-test-session" http://localhost:8080/api/projects/1
```

## 8. DeepSeek 验证样例

如果配置了真实 `DEEPSEEK_API_KEY`，建议分别用以下样例验证：

| 样例 | 输入重点 | 期望观察 |
| --- | --- | --- |
| 信息充分 | 目标用户、痛点、场景、商业模式完整 | `providerName=deepseek`，报告字段完整，多维评分可展示 |
| 信息模糊 | 只说“做一个让生活更轻松的 AI 应用” | 应提示信息不足，决策倾向 `PAUSE` |
| 高风险 | 校园社交 + 实时定位 + 广告赚钱 | 应指出隐私、冷启动、商业化风险 |

如果没有可用 key，不要把 Mock 成功当作 DeepSeek 成功。
