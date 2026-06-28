# 上线前检查清单

用于提交 GitHub 或部署到阿里云前的手动验收。

## 1. 本地运行

- [ ] 前端依赖安装成功：`cd frontend && pnpm install`
- [ ] 前端能启动：`pnpm dev`
- [ ] 前端能构建：`pnpm build`
- [ ] 后端能构建：`cd backend && ./mvnw -q -DskipTests package`
- [ ] 后端能启动：`./mvnw spring-boot:run`
- [ ] 数据库能连接并完成 `db/schema.sql` 初始化
- [ ] `GET /api/health` 返回成功

## 2. AI Provider

- [ ] 未配置 `DEEPSEEK_API_KEY` 时，`APP_AI_PROVIDER=auto` 能按预期走 Mock
- [ ] 配置 `DEEPSEEK_API_KEY` 后能真实生成报告
- [ ] DeepSeek 调用失败时 fallback 策略符合预期
- [ ] 错误信息不暴露 stack trace、真实 key 或 Provider 原始响应

## 3. 核心流程

- [ ] 点击“示例”能自动填入示例内容
- [ ] 示例自动选择“毒舌 PM + 直接”
- [ ] 点击“开始评审”后首页右侧进入 loading 状态
- [ ] 提交成功后首页右侧展示摘要
- [ ] 摘要包含 `goDecision`、决策原因、毒打指数、定位评分、成功指标和验证动作
- [ ] 点击“查看完整报告”进入详情页
- [ ] 详情页展示完整报告和验证计划
- [ ] 历史记录能看到刚才的评审
- [ ] 从历史记录再次点击能进入同一条详情

## 4. 复制与导出

- [ ] 首页可复制开发 Prompt
- [ ] 详情页可复制开发 Prompt
- [ ] 详情页可复制完整报告
- [ ] 详情页可导出 Markdown
- [ ] Markdown 包含决策结论、决策原因、成功指标、验证计划和开发 Prompt

## 5. Agent 质量

使用 [人工评测说明](eval.md) 的 3 个样例测试：

- [ ] 校园搭子小程序
- [ ] AI 产品评审工具
- [ ] 甲方校园服务小程序

检查：

- [ ] `goDecision` 有差异，不是所有样例都给同一种结论
- [ ] 毒打指数不都集中在 70 左右
- [ ] 产品定位评分能体现目标用户和场景是否清晰
- [ ] 成功指标可观察
- [ ] 验证计划可执行
- [ ] 开发 Prompt 能直接交给 Codex/Cursor

## 6. 移动端

- [ ] 首页不横向溢出
- [ ] 结果摘要不挤压
- [ ] 详情页章节可读
- [ ] 报告目录在窄屏可滚动或正常换行
- [ ] 按钮可点击，文字不溢出
- [ ] 历史记录列表可读

## 7. 安全

- [ ] 没有真实 API Key
- [ ] 没有真实数据库密码
- [ ] `.env` 未提交
- [ ] `application-local.yml` 未提交
- [ ] `application-dev.yml` 如包含真实配置则未提交
- [ ] `node_modules/` 未提交
- [ ] `.pnpm-store/` 未提交
- [ ] `frontend/dist/` 未提交
- [ ] `backend/target/` 未提交
- [ ] DeepSeek Key 只从后端环境变量或本地未提交配置读取
- [ ] 前端 bundle 不包含 `DEEPSEEK_API_KEY` 或真实 key

## 8. 部署前

- [ ] 阿里云安全组仅开放必要端口
- [ ] MySQL 使用应用专用账号，不使用 root
- [ ] 后端 jar 已生成
- [ ] 前端 dist 已生成
- [ ] Nginx 已配置 `/api/` 反向代理
- [ ] systemd 或其他后台运行方式已配置
- [ ] 上线前已备份数据库
