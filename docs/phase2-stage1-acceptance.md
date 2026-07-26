# 阶段 1 项目工作台验收记录

## 1. 验收结论

- 首次验收日期：2026-07-23
- 浏览器补验与最终回归日期：2026-07-26
- 当前结论：**阶段 1 验收通过**
- 原因：后端真实 MySQL 链路、V1/V2、项目聚合、跨 session 隔离、前端代理、真实浏览器导航、控制台检查及 1440x1000、390x844 双视口检查均已通过。
- 阶段判断：满足进入阶段 2 设计与开发的条件，但本轮未实现阶段 2 数据模型。

## 2. 验收环境

| 项目 | 实际配置 |
| --- | --- |
| 操作系统 | Windows |
| 后端端口 | `8081` |
| 前端端口 | `5173` |
| 数据库 | 本地 MySQL 8.0.45，已有数据的开发库 |
| AI Provider | `mock` |
| AI fallback | `false` |
| 前端代理目标 | `http://127.0.0.1:8081` |

数据库凭据仅通过进程环境传入，未写入仓库、文档或应用配置。连接前已只读检查目标库：`users`、`review_records`、`ai_call_logs` 均存在，阶段 1 所需字段齐全；库内已有数据，因此未执行 `db/schema.sql` 或迁移脚本。

## 3. 自动化基线

| 检查项 | 结果 |
| --- | --- |
| `backend/.\mvnw.cmd test` | 通过，34 个测试，0 失败，0 错误 |
| `frontend/pnpm build` | 通过，TypeScript 检查与 Vite 生产构建成功 |
| `GET /api/health` | 通过，返回 `UP` |

前端构建首次在受限沙箱中因 esbuild 无权读取上级目录失败；使用相同命令在获准环境中重跑后成功，未发现代码或类型错误。

## 4. 真实 MySQL 与 HTTP 链路

本次使用唯一验收 session 创建两条本地测试记录：V1 ID 为 `82`，V2 ID 为 `83`，`ideaGroupId` 为 `82`。这些 ID 仅用于本次本地验收，不是生产配置要求。

### V1

- `versionNo = 1`
- `parentReviewId = null`
- 首次创建时 `groupVersionCount = 1`
- `status = SUCCESS`
- `providerName = mock`
- 报告包含知识边界、质量审计和验证计划结构

### V2

- `versionNo = 2`
- `parentReviewId = 82`
- `ideaGroupId` 与 V1 一致
- `groupVersionCount = 2`
- `status = SUCCESS`
- `providerName = mock`
- V1 仍可读取，输入内容未被 V2 覆盖
- 分组接口稳定按 V1、V2 返回两个版本

数据库只读复核确认该 session 恰有两条 `review_records`，并有两条 `provider=mock`、`status=SUCCESS` 的 `ai_call_logs`。

## 5. 项目工作台聚合

`GET /api/projects/82` 的结构化断言全部通过：

- `projectId = ideaGroupId`
- `latestReviewId = 83`
- `latestVersionNo = 2`
- `totalVersions = 2`
- 当前决策、决策原因、评分、质量状态、质量摘要均与 V2 一致
- 关键假设 3 条、未知问题 3 条、验证动作 4 条，均与 V2 报告一致
- 成功指标来自 V2
- `versions` 按 V1、V2 顺序返回

## 6. 跨 session 隔离

使用不同 session 请求 session A 的资源，以下接口均返回 HTTP `404`、业务码 `NOT_FOUND`、`data = null`：

- `GET /api/projects/82`
- `GET /api/reviews/group/82`
- `GET /api/reviews/82`
- `GET /api/reviews/83`

错误响应仅包含 `success`、`code`、`message`、`data` 四个标准字段，未泄露目标资源的额外信息。

## 7. 前端真实代理

- `GET http://127.0.0.1:5173/api/projects/82` 在 session A 下返回 HTTP 200、V2 当前版本和两个版本记录。
- 同一代理请求在 session B 下返回 HTTP `404/NOT_FOUND`。
- `/reviews/83`、`/projects/82`、`/ideas/82` 三个 SPA 深链路均返回应用入口页面。

## 8. 真实浏览器与响应式检查

使用 Playwright 和本机 Chrome 完成以下真实浏览器链路：

- V2 详情页 -> 项目工作台。
- 项目工作台 -> 最新完整报告 -> 返回项目工作台。
- 项目工作台 -> 版本对比 -> 返回项目工作台。
- 刷新 `/projects/82` 后从后端恢复 V2 聚合与 V1/V2 时间线。
- 独立 session B 访问项目路由时显示“项目不存在”，不渲染 session A 数据。

桌面与移动端结果：

| 检查项 | 1440x1000 | 390x844 |
| --- | --- | --- |
| 横向溢出 | 无 | 无 |
| 操作按钮重叠 | 无 | 无 |
| 操作文字裁切 | 无 | 无 |
| 累积布局偏移 | `0` | `0` |
| 未处理页面异常 | `0` | `0` |
| 非预期失败请求 | `0` | `0` |
| 主链路控制台错误 | `0` | `0` |

session B 仅出现预期的项目接口 `404` 和前端已处理的 API 错误日志。桌面与移动端全页截图经目视复核，未发现内容遮挡、时间线错位或明显布局跳动。

## 9. 代码修改

浏览器检查发现 `/favicon.ico` 缺失会持续产生控制台 404。已在 `frontend/index.html` 增加内联品牌 favicon，修复后完整浏览器验收和前端生产构建均通过。

本轮未修改 AI Provider、Prompt、Parser 或相关业务逻辑。除本验收记录外，仅修改 `frontend/index.html` 修复 favicon。
