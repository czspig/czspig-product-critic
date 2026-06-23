# 项目开发记录：czspig-product-critic - 产品架构设计与后端 Mock Agent 基础闭环

## 一、本轮开发概览

- 项目名称：czspig-product-critic
- 项目类型：AI 产品评审 Agent Web 应用
- 本轮模块：产品与技术架构设计、Spring Boot 后端基础闭环、Mock Agent、MySQL 数据库脚本、项目文档
- 开发阶段：Demo
- 本轮目标：先用 agent dev skill 梳理产品架构，再完成后端基础接口、Mock Agent 评审流程和数据库设计，跑通“输入产品想法 -> 生成评审报告 -> 保存历史 -> 查询详情”的服务端闭环。
- 完成状态：部分完成。代码和文档已生成；由于本机缺少 Maven，尚未完成真实编译、启动和接口运行验证。

## 二、原始 Prompt 记录

### Prompt 1：产品与技术架构设计

````text
agent创建提示词
# 任务：使用 agent dev skill 为「czspig-product-critic」设计产品与技术架构 
你现在需要基于我已经创建的，先不要写业务代码，先完成产品定位、Agent 架构、页面结构、后端结构、数据库结构和开发计划设计。 
## 一、必须使用的 Skill 
请优先读取并遵循当前项目中的 agent dev skill，尤其是以下文件： 
- `SKILL.md` 
- `references/app-architecture.md` 
- `references/output-templates.md` 
如果这些文件存在，请先阅读它们，再开始分析。 
本阶段只做架构和方案设计，不要直接进入完整编码。 
## 二、项目背景 
我要开发一个可上线的 AI 产品评审 Agent Web 应用。 
项目名称：czspig-product-critic 
产品定位： 
一个“犀利但鼓励”的 AI 产品经理 Agent。用户输入产品想法、创业点子、功能需求或需求文档后，Agent 会从产品经理视角进行评审，指出伪需求、功能冗余、用户痛点不足、冷启动风险，并生成 MVP 改造方案和给 Codex/Cursor 的开发 Prompt。 
产品气质： 
- 犀利毒舌，但不恶意打击 
- 温暖、克制、含蓄 
- 鼓励性表达 
- 参考 Claude 的颜色设计风格 
- 不做夸张娱乐风 
- 不做普通聊天框套壳 
首页形态： 
- 不做营销落地页 
- 打开就是核心输入区 
- 用户可以直接输入产品想法并开始评审 
第一版技术栈： 
- 前端：Vue3 + Vite + TypeScript 
- 后端：Spring Boot 3 + Java 17+ 
- 数据库：MySQL 
- AI：DeepSeek API 
- 部署：先推送 GitHub，后续部署到阿里云 
第一版功能： 
- 登录入口预留，但不实现完整登录 
- 历史记录需要保存到数据库 
- 支持导出 Markdown 
- 不做分享链接 
- 不做支付 
- 不做复杂后台 
评审模式： 
1. 温和导师 
2. 犀利 PM 
3. 甲方视角 
吐槽强度： 
1. 温和 
2. 正常 
3. 毒舌 
评审报告必须包含： 
1. 一句话评价 
2. 毒打指数，0-100 
3. 产品定位评分 
4. 用户痛点分析 
5. 伪需求风险 
6. 功能冗余检查 
7. 冷启动问题 
8. MVP 改造建议 
9. 最小可开发版本 
10. 给 Codex/Cursor 的开发 Prompt 
## 三、本阶段输出要求 
请输出一份 Markdown 架构设计文档，包含： 
1. 产品定位判断 
   - 这个产品第一版应该做什么 
   - 不应该做什么 
   - 如何避免变成普通聊天机器人 
2. 用户流程 
   - 从用户进入首页到生成报告的完整流程 
   - 历史记录查看流程 
   - Markdown 导出流程 
3. 页面结构 
   - 首页 
   - 评审结果页 
   - 历史记录页 
   - 关于页 
   - 登录占位状态 
4. 前端架构 
   - 推荐目录结构 
   - 组件划分 
   - 状态管理设计 
   - API 封装设计 
   - Markdown 渲染方案 
5. 后端架构 
   - Controller 
   - Service 
   - AI Provider 
   - Prompt Builder 
   - Entity 
   - Mapper 
   - DTO 
   - Config 
   - Common Response 
6. 数据库设计 
   - users 表 
   - review_records 表 
   - ai_call_logs 表 
   - 必要字段和说明 
7. 核心接口设计 
   - 创建评审 
   - 获取历史记录 
   - 获取评审详情 
   - 健康检查接口 
8. Agent Prompt 结构 
   - system prompt 设计 
   - mode 参数如何影响语气 
   - roastLevel 参数如何影响强度 
   - 如何保证“犀利但鼓励” 
   - 如何保证输出结构稳定 
9. 第一版开发任务拆分 
   - 阶段 1：项目初始化 
   - 阶段 2：后端基础接口 
   - 阶段 3：DeepSeek 接入 
   - 阶段 4：前端页面 
   - 阶段 5：历史记录 
   - 阶段 6：导出 Markdown 
   - 阶段 7：联调和文档 
10. 风险点 
    - API Key 泄露风险 
    - AI 输出不稳定风险 
    - 提示词注入风险 
    - 数据库存储风险 
    - 前后端联调风险 
## 四、限制 
- 不要直接写完整代码 
- 不要做登录、支付、分享链接 
- 不要引入复杂微服务 
- 不要过度设计后台管理 
- 不要把它做成普通 ChatGPT 对话框 
- 不要忽略我的 agent dev skill 中的规范 
## 五、最后输出 
最后请给出： 
1. 推荐的项目目录结构 
2. 第一阶段应该让 Codex 执行的下一条开发提示词 
3. 你认为当前需求中还需要我确认的问题
````

### Prompt 2：第二阶段后端基础闭环

````text
agent创建提示词
下面这条是下一阶段 Codex 提示词。这一轮建议做：后端基础接口 + Mock Agent 评审流程 + 数据库 SQL，先不接 DeepSeek。这样可以先把“输入 → 生成报告 → 保存历史 → 查询详情”闭环跑通。
Markdown# 任务：使用 agent dev skill 完成 czspig-product-critic 第二阶段后端基础闭环你现在继续开发 `czspig-product-critic` 项目。请注意：本项目必须结合我已经创建的 `agent dev skill` 来推进，不要只按普通 Web 项目开发。## 一、必须读取的 Skill 文件开始前请先读取并遵循以下文件：- `SKILL.md`- `references/app-architecture.md`- `references/tool-contract-design.md`- `references/output-templates.md`- `references/security-guardrails-review.md`本阶段重点参考：- `app-architecture.md`：确认应用架构、边界、模块职责- `tool-contract-design.md`：确认接口命名、请求参数、返回结构、错误边界- `output-templates.md`：确认评审报告输出结构- `security-guardrails-review.md`：确认 API Key、日志、用户输入、提示词注入的安全边界如果当前仓库中已经有 `docs/product-architecture.md`，也必须先阅读。## 二、项目固定信息英文项目名 / 仓库名：`czspig-product-critic`中文产品名 / 页面展示名：`猪猪产品毒舌官`产品定位：一个“犀利但鼓励”的 AI 产品经理评审 Agent。用户输入产品想法、创业点子、功能需求或需求文档后，系统从产品经理视角进行评审，指出伪需求、功能冗余、用户痛点不足、冷启动风险，并生成 MVP 改造方案和给 Codex/Cursor 的开发 Prompt。注意：- 它不是普通聊天机器人- 它不是自由问答工具- 它是结构化产品评审 Agent- 第一版必须围绕“输入产品想法 → 生成评审报告 → 保存历史 → 查看历史 → 导出 Markdown”这个闭环设计## 三、本阶段目标本阶段只完成后端基础闭环和数据库设计，不接入真实 DeepSeek API。请完成：1. 后端基础分层结构2. 统一 API 响应结构3. 全局异常处理4. 健康检查接口5. Review 创建接口6. Review 历史列表接口7. Review 详情接口8. Mock AI 评审服务9. MySQL 初始化 SQL10. 基础 README / docs 更新## 四、后端技术要求技术栈：- Spring Boot 3- Java 17+- MyBatis 或 MyBatis Plus，优先根据当前项目已有依赖判断- MySQL- Maven如果项目目前还没有选择 MyBatis 或 MyBatis Plus，请优先使用 MyBatis Plus，减少基础 CRUD 代码量。## 五、后端目录结构要求请根据当前项目结构小步扩展，不要粗暴重建。建议结构如下：```textbackend/  src/main/java/com/czspig/productcritic/    ProductCriticApplication.java    common/      ApiResponse.java      ErrorCode.java      BizException.java      GlobalExceptionHandler.java    config/      CorsConfig.java    controller/      HealthController.java      ReviewController.java    service/      ReviewService.java      impl/        ReviewServiceImpl.java    ai/      AiProvider.java      MockAiProvider.java      ProductReviewPromptBuilder.java      ProductReviewOutputParser.java    dto/      CreateReviewRequest.java      ReviewDetailResponse.java      ReviewListItemResponse.java      ReviewReportDto.java    entity/      ReviewRecordEntity.java      UserEntity.java      AiCallLogEntity.java    mapper/      ReviewRecordMapper.java      UserMapper.java      AiCallLogMapper.java
如果当前已有包名或目录，请优先沿用当前项目风格。
六、接口设计
1. 健康检查
GET /api/health
返回示例：
JSON{  "success": true,  "code": "OK",  "message": "success",  "data": {    "status": "UP",    "service": "czspig-product-critic"  }}
2. 创建评审
POST /api/reviews
请求：
JSON{  "content": "我想做一个帮助大学生找搭子的校园小程序",  "mode": "SHARP_PM",  "roastLevel": 2}
字段说明：
content：用户输入的产品想法，必填，长度建议 10-5000
mode：
MENTOR：温和导师
SHARP_PM：犀利 PM
CLIENT：甲方视角
roastLevel：
1：温和
2：正常
3：毒舌
返回：
JSON{  "success": true,  "code": "OK",  "message": "success",  "data": {    "id": 1,    "oneLineVerdict": "这个想法有场景，但现在还像功能清单，不像一个真正的产品切口。",    "beatScore": 72,    "positioningScore": 68,    "reportMarkdown": "...",    "createdAt": "2026-06-23 12:00:00"  }}
3. 获取历史记录
GET /api/reviews?page=1&pageSize=10
返回列表字段：
id
inputSummary
mode
roastLevel
oneLineVerdict
beatScore
positioningScore
createdAt
4. 获取评审详情
GET /api/reviews/{id}
返回完整字段：
用户原始输入
评审模式
吐槽强度
一句话评价
毒打指数
产品定位评分
结构化报告
Markdown 报告
创建时间
七、Mock Agent 输出要求
本阶段不要接 DeepSeek。
请先实现 MockAiProvider，让后端可以返回稳定的结构化评审报告。
Mock 报告必须包含以下 10 个模块：
一句话评价
毒打指数，0-100
产品定位评分
用户痛点分析
伪需求风险
功能冗余检查
冷启动问题
MVP 改造建议
最小可开发版本
给 Codex/Cursor 的开发 Prompt
Mock 内容不要太敷衍，要能真实展示产品效果。
语气要求：
犀利但鼓励
温暖克制
不攻击用户本人
不低俗
不输出空泛套话
八、数据库设计
请创建：
db/schema.sql
至少包含三张表：
users
review_records
ai_call_logs
review_records 至少包含：
id
user_id
session_id
input_content
input_summary
mode
roast_level
one_line_verdict
beat_score
positioning_score
report_json
report_markdown
status
error_message
model_name
prompt_version
created_at
updated_at
deleted_at
要求：
不保存任何真实 API Key
不设计复杂登录
第一版允许使用默认匿名 session
SQL 要能直接在 MySQL 执行
九、安全和边界要求
请根据 security-guardrails-review.md 做基础安全处理：
用户输入需要长度校验
枚举参数需要校验
错误信息不要暴露 Java stack trace
不要在日志中打印敏感信息
不要硬编码任何 AI API Key
本阶段不允许外部写操作
Prompt Builder 中要预留提示词注入防护逻辑，但本阶段不用真实调用模型
十、文档更新
请更新或创建：
README.md
docs/api.md
docs/database-schema.md
文档至少说明：
当前阶段完成内容
后端启动方式
MySQL 初始化方式
API 列表
当前使用 Mock AI
下一阶段将接入 DeepSeek API
十一、验证要求
完成后请尽量执行：
后端编译或测试
健康检查接口验证
创建评审接口验证
历史记录接口验证
详情接口验证
如果因为环境缺失无法运行，请明确说明原因，并给出我在本地应该执行的命令。
十二、不要做的事情
本阶段不要做：
不接入真实 DeepSeek API
不实现完整登录
不做支付
不做分享链接
不做复杂后台管理
不做 RAG
不做多 Agent 编排
不重构无关文件
不改变项目名
十三、最终回复格式
请按以下格式回复：
本阶段完成了什么
创建或修改了哪些文件
如何启动后端
如何初始化数据库
如何测试接口
当前风险或未完成项
下一阶段建议
最后，请给出下一阶段 Codex 提示词草案：接入 DeepSeek API Provider。
Plain Text这一轮的重点是：**先用 Mock Agent 把产品闭环跑起来**。等这步完成后，下一轮再让 Codex 接 DeepSeek，不然前端、后端、AI、数据库一起上，出问题不好定位。
下面这条是下一阶段 Codex 提示词。这一轮建议做：后端基础接口 + Mock Agent 评审流程 + 数据库 SQL，先不接 DeepSeek。这样可以先把“输入 → 生成报告 → 保存历史 → 查询详情”闭环跑通。
Markdown# 任务：使用 agent dev skill 完成 czspig-product-critic 第二阶段后端基础闭环你现在继续开发 `czspig-product-critic` 项目。请注意：本项目必须结合我已经创建的 `agent dev skill` 来推进，不要只按普通 Web 项目开发。## 一、必须读取的 Skill 文件开始前请先读取并遵循以下文件：- `SKILL.md`- `references/app-architecture.md`- `references/tool-contract-design.md`- `references/output-templates.md`- `references/security-guardrails-review.md`本阶段重点参考：- `app-architecture.md`：确认应用架构、边界、模块职责- `tool-contract-design.md`：确认接口命名、请求参数、返回结构、错误边界- `output-templates.md`：确认评审报告输出结构- `security-guardrails-review.md`：确认 API Key、日志、用户输入、提示词注入的安全边界如果当前仓库中已经有 `docs/product-architecture.md`，也必须先阅读。## 二、项目固定信息英文项目名 / 仓库名：`czspig-product-critic`中文产品名 / 页面展示名：`猪猪产品毒舌官`产品定位：一个“犀利但鼓励”的 AI 产品经理评审 Agent。用户输入产品想法、创业点子、功能需求或需求文档后，系统从产品经理视角进行评审，指出伪需求、功能冗余、用户痛点不足、冷启动风险，并生成 MVP 改造方案和给 Codex/Cursor 的开发 Prompt。注意：- 它不是普通聊天机器人- 它不是自由问答工具- 它是结构化产品评审 Agent- 第一版必须围绕“输入产品想法 → 生成评审报告 → 保存历史 → 查看历史 → 导出 Markdown”这个闭环设计## 三、本阶段目标本阶段只完成后端基础闭环和数据库设计，不接入真实 DeepSeek API。请完成：1. 后端基础分层结构2. 统一 API 响应结构3. 全局异常处理4. 健康检查接口5. Review 创建接口6. Review 历史列表接口7. Review 详情接口8. Mock AI 评审服务9. MySQL 初始化 SQL10. 基础 README / docs 更新## 四、后端技术要求技术栈：- Spring Boot 3- Java 17+- MyBatis 或 MyBatis Plus，优先根据当前项目已有依赖判断- MySQL- Maven如果项目目前还没有选择 MyBatis 或 MyBatis Plus，请优先使用 MyBatis Plus，减少基础 CRUD 代码量。## 五、后端目录结构要求请根据当前项目结构小步扩展，不要粗暴重建。建议结构如下：```textbackend/  src/main/java/com/czspig/productcritic/    ProductCriticApplication.java    common/      ApiResponse.java      ErrorCode.java      BizException.java      GlobalExceptionHandler.java    config/      CorsConfig.java    controller/      HealthController.java      ReviewController.java    service/      ReviewService.java      impl/        ReviewServiceImpl.java    ai/      AiProvider.java      MockAiProvider.java      ProductReviewPromptBuilder.java      ProductReviewOutputParser.java    dto/      CreateReviewRequest.java      ReviewDetailResponse.java      ReviewListItemResponse.java      ReviewReportDto.java    entity/      ReviewRecordEntity.java      UserEntity.java      AiCallLogEntity.java    mapper/      ReviewRecordMapper.java      UserMapper.java      AiCallLogMapper.java
如果当前已有包名或目录，请优先沿用当前项目风格。
六、接口设计
1. 健康检查
GET /api/health
返回示例：
JSON{  "success": true,  "code": "OK",  "message": "success",  "data": {    "status": "UP",    "service": "czspig-product-critic"  }}
2. 创建评审
POST /api/reviews
请求：
JSON{  "content": "我想做一个帮助大学生找搭子的校园小程序",  "mode": "SHARP_PM",  "roastLevel": 2}
字段说明：
content：用户输入的产品想法，必填，长度建议 10-5000
mode：
MENTOR：温和导师
SHARP_PM：犀利 PM
CLIENT：甲方视角
roastLevel：
1：温和
2：正常
3：毒舌
返回：
JSON{  "success": true,  "code": "OK",  "message": "success",  "data": {    "id": 1,    "oneLineVerdict": "这个想法有场景，但现在还像功能清单，不像一个真正的产品切口。",    "beatScore": 72,    "positioningScore": 68,    "reportMarkdown": "...",    "createdAt": "2026-06-23 12:00:00"  }}
3. 获取历史记录
GET /api/reviews?page=1&pageSize=10
返回列表字段：
id
inputSummary
mode
roastLevel
oneLineVerdict
beatScore
positioningScore
createdAt
4. 获取评审详情
GET /api/reviews/{id}
返回完整字段：
用户原始输入
评审模式
吐槽强度
一句话评价
毒打指数
产品定位评分
结构化报告
Markdown 报告
创建时间
七、Mock Agent 输出要求
本阶段不要接 DeepSeek。
请先实现 MockAiProvider，让后端可以返回稳定的结构化评审报告。
Mock 报告必须包含以下 10 个模块：
一句话评价
毒打指数，0-100
产品定位评分
用户痛点分析
伪需求风险
功能冗余检查
冷启动问题
MVP 改造建议
最小可开发版本
给 Codex/Cursor 的开发 Prompt
Mock 内容不要太敷衍，要能真实展示产品效果。
语气要求：
犀利但鼓励
温暖克制
不攻击用户本人
不低俗
不输出空泛套话
八、数据库设计
请创建：
db/schema.sql
至少包含三张表：
users
review_records
ai_call_logs
review_records 至少包含：
id
user_id
session_id
input_content
input_summary
mode
roast_level
one_line_verdict
beat_score
positioning_score
report_json
report_markdown
status
error_message
model_name
prompt_version
created_at
updated_at
deleted_at
要求：
不保存任何真实 API Key
不设计复杂登录
第一版允许使用默认匿名 session
SQL 要能直接在 MySQL 执行
九、安全和边界要求
请根据 security-guardrails-review.md 做基础安全处理：
用户输入需要长度校验
枚举参数需要校验
错误信息不要暴露 Java stack trace
不要在日志中打印敏感信息
不要硬编码任何 AI API Key
本阶段不允许外部写操作
Prompt Builder 中要预留提示词注入防护逻辑，但本阶段不用真实调用模型
十、文档更新
请更新或创建：
README.md
docs/api.md
docs/database-schema.md
文档至少说明：
当前阶段完成内容
后端启动方式
MySQL 初始化方式
API 列表
当前使用 Mock AI
下一阶段将接入 DeepSeek API
十一、验证要求
完成后请尽量执行：
后端编译或测试
健康检查接口验证
创建评审接口验证
历史记录接口验证
详情接口验证
如果因为环境缺失无法运行，请明确说明原因，并给出我在本地应该执行的命令。
十二、不要做的事情
本阶段不要做：
不接入真实 DeepSeek API
不实现完整登录
不做支付
不做分享链接
不做复杂后台管理
不做 RAG
不做多 Agent 编排
不重构无关文件
不改变项目名
十三、最终回复格式
请按以下格式回复：
本阶段完成了什么
创建或修改了哪些文件
如何启动后端
如何初始化数据库
如何测试接口
当前风险或未完成项
下一阶段建议
最后，请给出下一阶段 Codex 提示词草案：接入 DeepSeek API Provider。
Plain Text这一轮的重点是：**先用 Mock Agent 把产品闭环跑起来**。等这步完成后，下一轮再让 Codex 接 DeepSeek，不然前端、后端、AI、数据库一起上，出问题不好定位。
````

## 三、Codex 输出摘要

做了什么：

- 按 `agent-app-development-workflow` skill 读取并使用 `app-architecture`、`output-templates`、`tool-contract-design`、`security-guardrails-review` 等参考文件。
- 第一轮先做架构设计，没有写业务代码，生成产品与技术架构文档。
- 第二轮实现 Spring Boot 3 后端基础结构，使用 MyBatis Plus、MySQL、Mock Agent Provider，先不接 DeepSeek。
- 建立统一响应、全局异常处理、健康检查接口、Review 创建接口、历史列表接口、详情接口。
- 生成 MySQL 初始化脚本和 README/API/数据库文档。
- 检查本地环境：Java 可用，Maven 不在 PATH 中；当前 `.git` 目录不是有效 Git 仓库。

改了什么：

- 新增架构设计文档：`docs/product-architecture.md`。
- 新增 Maven 聚合配置：`pom.xml`。
- 新增后端 Maven 项目：`backend/pom.xml`。
- 新增 Spring Boot 后端源码：`backend/src/main/java/com/czspig/productcritic/**`。
- 新增配置文件：`backend/src/main/resources/application.yml`、`application-example.yml`。
- 新增数据库脚本：`db/schema.sql`。
- 新增文档：`README.md`、`docs/api.md`、`docs/database-schema.md`。
- 新增忽略规则：`.gitignore`。

运行了什么：

- `rg --files`
- `git status --short`
- `java -version`
- `mvn -version`
- `mvn test`
- `Select-String` 检查文档章节、接口路径、关键字段和敏感 key 形态
- PowerShell 读取 docx、skill 和 reference 文件

结果如何：

- `docs/product-architecture.md` 已覆盖产品定位、用户流程、页面结构、前端架构、后端架构、数据库设计、接口设计、Agent Prompt、开发计划和风险。
- 后端基础代码已生成，包含 `GET /api/health`、`POST /api/reviews`、`GET /api/reviews`、`GET /api/reviews/{id}`。
- Mock 报告包含 10 个要求模块，并通过 `ProductReviewOutputParser` 做字段完整性与分数范围校验。
- `db/schema.sql` 包含 `users`、`review_records`、`ai_call_logs` 三张表。
- 未发现真实 API Key 形态内容。
- `mvn test` 未运行成功，原因是本机未安装 Maven 或 Maven 未加入 PATH。
- `git status --short` 未运行成功，原因是当前目录存在 `.git` 目录但不是有效 Git 仓库。

未解决问题：

- 需要安装 Maven 后执行真实编译、测试和接口启动验证。
- 需要初始化 MySQL 并确认 `schema.sql` 在目标 MySQL 版本上执行无误。
- 需要后续接入 DeepSeek API Provider，并确认 DeepSeek 官方接口、模型名、响应格式和 JSON 输出能力。
- 需要后续补齐前端 Vue3 页面与 Markdown 导出交互。
- 当前没有可用飞书/Lark 连接器，无法由 Codex 自动发布到飞书知识库。

## 四、实际开发动作记录

| 动作 | 目的 | 结果 |
|---|---|---|
| 读取 `agent-app-development-workflow` skill 与相关 references | 确认 Agent 应用开发阶段、边界、安全要求和输出模板 | 已读取并按阶段拆分执行 |
| 提取 `C:\Users\hp\Desktop\agent创建提示词.docx` | 获取第一轮架构设计原始 prompt | 已完整提取并用于生成架构文档 |
| 创建 `docs/product-architecture.md` | 固化产品定位、系统架构、接口、数据库、Prompt 结构和风险 | 文档已生成，章节覆盖完整 |
| 提取 `C:\Users\hp\Downloads\agent创建提示词.docx` | 获取第二轮后端基础闭环开发 prompt | 已完整提取并作为实现依据 |
| 创建 Spring Boot 后端项目结构 | 承接第二阶段“后端基础闭环”目标 | 已创建 `backend/`、`pom.xml`、启动类和分层目录 |
| 实现统一响应与异常处理 | 保证 API 返回格式稳定，避免暴露 Java stack trace | 已创建 `ApiResponse`、`ErrorCode`、`BizException`、`GlobalExceptionHandler` |
| 实现 Review 接口 | 支撑创建评审、查询历史、查询详情 | 已创建 `ReviewController`、`ReviewService`、`ReviewServiceImpl` |
| 实现 `MockAiProvider` | 在不接 DeepSeek 的情况下跑通 Agent 评审效果 | 已生成稳定 Mock 报告，包含 10 个模块 |
| 实现 Prompt Builder 与输出校验 | 预留提示词注入防护逻辑，保证报告结构稳定 | 已创建 `ProductReviewPromptBuilder` 与 `ProductReviewOutputParser` |
| 创建 MySQL 初始化脚本 | 支撑历史记录与 AI 调用日志持久化 | 已创建 `db/schema.sql`，包含三张表 |
| 更新 README 和 docs | 说明启动方式、API、数据库和下一阶段 | 已创建 `README.md`、`docs/api.md`、`docs/database-schema.md` |
| 尝试运行 `mvn test` | 验证后端是否可编译测试 | 失败，原因是 `mvn` 命令不存在 |
| 检查 Git 状态 | 确认工作区变更 | 失败，原因是当前目录不是有效 Git 仓库 |

## 五、涉及文件与模块

| 文件/模块 | 作用 | 本轮变化 |
|---|---|---|
| `docs/product-architecture.md` | 产品与技术架构设计文档 | 新增，作为第一阶段方案文档 |
| `pom.xml` | 根目录 Maven 聚合配置 | 新增，聚合 `backend` 模块 |
| `backend/pom.xml` | 后端 Maven 配置 | 新增 Spring Boot 3、Validation、MyBatis Plus、MySQL 依赖 |
| `backend/src/main/java/com/czspig/productcritic/ProductCriticApplication.java` | Spring Boot 启动入口 | 新增 |
| `backend/src/main/java/com/czspig/productcritic/controller/HealthController.java` | 健康检查接口 | 新增 `GET /api/health` |
| `backend/src/main/java/com/czspig/productcritic/controller/ReviewController.java` | Review API 控制层 | 新增创建评审、历史列表、详情接口 |
| `backend/src/main/java/com/czspig/productcritic/service/ReviewService.java` | Review 服务接口 | 新增服务契约 |
| `backend/src/main/java/com/czspig/productcritic/service/impl/ReviewServiceImpl.java` | Review 核心业务实现 | 新增 Mock 评审、保存记录、分页查询、详情查询、AI 调用日志 |
| `backend/src/main/java/com/czspig/productcritic/ai/AiProvider.java` | AI Provider 抽象 | 新增，便于后续替换 DeepSeek |
| `backend/src/main/java/com/czspig/productcritic/ai/MockAiProvider.java` | Mock Agent 实现 | 新增稳定结构化报告生成 |
| `backend/src/main/java/com/czspig/productcritic/ai/ProductReviewPromptBuilder.java` | Agent Prompt 构建 | 新增安全边界和提示词注入防护预留 |
| `backend/src/main/java/com/czspig/productcritic/ai/ProductReviewOutputParser.java` | 报告结构校验 | 新增必填字段和分数范围校验 |
| `backend/src/main/java/com/czspig/productcritic/dto/**` | 请求/响应 DTO | 新增创建请求、详情响应、列表响应、报告结构 |
| `backend/src/main/java/com/czspig/productcritic/entity/**` | 数据库实体 | 新增用户、评审记录、AI 调用日志实体 |
| `backend/src/main/java/com/czspig/productcritic/mapper/**` | MyBatis Plus Mapper | 新增三张表 Mapper |
| `backend/src/main/java/com/czspig/productcritic/common/**` | 通用响应、错误码、异常、分页、枚举 | 新增 API 通用基础设施 |
| `backend/src/main/resources/application.yml` | 后端默认配置 | 新增端口、MySQL、Jackson、匿名 session 配置 |
| `backend/src/main/resources/application-example.yml` | 配置示例 | 新增，不包含真实密钥 |
| `db/schema.sql` | MySQL 初始化脚本 | 新增 `users`、`review_records`、`ai_call_logs` |
| `README.md` | 项目说明 | 新增当前阶段、启动方式、数据库初始化、安全边界 |
| `docs/api.md` | API 文档 | 新增接口说明和 curl 示例 |
| `docs/database-schema.md` | 数据库文档 | 新增表结构说明 |
| `.gitignore` | 忽略规则 | 新增本地配置、构建产物、日志和前端产物忽略规则 |

## 六、本轮完成内容

- 完成 AI 产品评审 Agent 的产品定位和技术架构设计。
- 明确第一版不是普通聊天机器人，而是结构化产品评审工作台。
- 明确第一版围绕输入、报告、历史、导出 Markdown 的闭环推进。
- 创建 Spring Boot 3 后端基础项目和分层结构。
- 实现统一 API 响应和全局异常处理。
- 实现健康检查接口、Review 创建接口、历史列表接口和详情接口。
- 实现 `MockAiProvider`，在不接 DeepSeek 的情况下生成稳定结构化报告。
- 创建 MySQL 初始化脚本，覆盖用户、评审记录和 AI 调用日志三张表。
- 更新 README、API 文档和数据库文档。
- 明确下一阶段为 DeepSeek API Provider 接入。

## 七、问题、风险与待确认事项

- Maven 未安装或未加入 PATH，导致无法运行 `mvn test`、无法启动后端做接口实测。
- 当前 `.git` 目录不是有效 Git 仓库，无法查看 Git 状态或提交记录。
- 数据库脚本尚未在真实 MySQL 环境执行，需要用户本地初始化验证。
- DeepSeek API 的模型名、接口字段、响应格式、JSON 输出能力和超时策略仍需查官方文档确认。
- 当前只完成后端 Mock 闭环，前端 Vue3 页面、Markdown 导出按钮和前后端联调尚未完成。
- 第一版匿名历史使用 `X-Session-Id`/默认 session 隔离，正式上线前需要设计真实登录和权限边界。
- 当前 Codex 环境没有飞书/Lark 知识库连接器，无法自动写入飞书知识库；需要手动复制本文，或后续提供飞书 API/Webhook/知识库权限配置。

## 八、项目进度判断

- 当前判断：Demo
- 判断依据：项目已有架构文档、后端 Mock Agent 代码、数据库脚本和 API 文档，能展示核心服务端闭环设计；但没有 Maven 编译结果、没有数据库初始化验证、没有真实接口运行结果，也没有前端页面和 DeepSeek 接入，因此不能判断为 MVP、可测试或可交付。

## 九、下一步计划

1. 安装 Maven 或使用 IDE 导入 `backend/pom.xml`，执行 `mvn test` 或 `mvn package`，修复可能出现的编译问题。
2. 初始化 MySQL：执行 `mysql -u root -p < db/schema.sql`，确认三张表可创建。
3. 启动后端并用 `curl` 验证 `GET /api/health`、`POST /api/reviews`、`GET /api/reviews`、`GET /api/reviews/{id}`。
4. 接入 DeepSeek API Provider，保留 Mock Provider 作为本地 fallback。
5. 实现 Vue3 前端页面，先完成首页输入、结果展示、历史列表和详情页。
6. 实现前端 Markdown 导出，并与后端结构化报告字段联调。
7. 在 Git 仓库重新初始化或修复 `.git` 后，建立提交记录和后续变更追踪。

## 十、可继续发给 Codex 的提示词

```markdown
你是 Codex，请在当前仓库中接入 DeepSeek API Provider。先读取以下上下文后再修改代码：

- C:\Users\hp\Desktop\czspig-product-critic\README.md
- C:\Users\hp\Desktop\czspig-product-critic\docs\product-architecture.md
- C:\Users\hp\Desktop\czspig-product-critic\docs\api.md
- C:\Users\hp\Desktop\czspig-product-critic\docs\database-schema.md
- C:\Users\hp\Desktop\czspig-product-critic\backend\pom.xml
- C:\Users\hp\Desktop\czspig-product-critic\backend\src\main\java\com\czspig\productcritic\ai
- C:\Users\hp\Desktop\czspig-product-critic\backend\src\main\java\com\czspig\productcritic\service\impl\ReviewServiceImpl.java

## 当前项目上下文

项目名：czspig-product-critic
中文产品名：猪猪产品毒舌官
产品定位：犀利但鼓励的 AI 产品经理评审 Agent。

当前已完成：
- Spring Boot 3 + Java 17+ 后端基础结构
- MyBatis Plus + MySQL 表设计
- 统一 ApiResponse 响应
- 全局异常处理
- 健康检查接口
- Review 创建、历史列表、详情接口
- MockAiProvider
- ProductReviewPromptBuilder
- ProductReviewOutputParser
- db/schema.sql
- README、API 文档和数据库文档

## 下一轮目标

新增 DeepSeekAiProvider，实现真实 AI 评审能力，但必须保留 Mock Provider 作为本地开发 fallback。

## 任务列表

1. 先检查现有代码结构、依赖、配置文件和运行方式。
2. 查阅 DeepSeek 官方文档，确认当前 API 的请求 URL、鉴权方式、模型名、请求字段、响应字段和错误格式；不要凭空编造 API。
3. 新增配置类，例如 `DeepSeekProperties`，从环境变量读取：
   - `DEEPSEEK_API_KEY`
   - `DEEPSEEK_BASE_URL`
   - `DEEPSEEK_MODEL`
   - `DEEPSEEK_TIMEOUT_SECONDS`
4. 新增 `DeepSeekAiProvider`，实现现有 `AiProvider` 接口。
5. 增加 provider 切换配置，例如 `APP_AI_PROVIDER=mock|deepseek`。
6. 让 DeepSeek 输出稳定 JSON，并复用 `ProductReviewOutputParser` 校验字段。
7. AI 输出 JSON 解析失败时返回友好错误，不暴露 stack trace。
8. AI 调用日志只保存 prompt hash、脱敏摘要、模型名、耗时和状态，不保存 API Key。
9. 更新 `README.md` 和 `docs/api.md`，说明 DeepSeek 配置、启动方式和 Mock fallback。

## 约束条件

- 不提交真实 API Key。
- 不把 API Key 写入前端、日志、AI 上下文或错误响应。
- 不实现登录、支付、分享链接、复杂后台、RAG、多 Agent。
- 不重构无关模块。
- 不改变项目名和现有接口路径。
- 如果本地缺 Maven 或网络无法下载依赖，要明确说明阻塞原因和用户本地执行命令。

## 验证方式

- 优先执行 `mvn test` 或 `mvn package`。
- 如果可以启动后端，验证：
  - `GET /api/health`
  - `POST /api/reviews`
  - `GET /api/reviews?page=1&pageSize=10`
  - `GET /api/reviews/{id}`
- 验证 Mock Provider 仍可用。
- 验证未提交真实密钥。

## 输出要求

请最终回复：
- 本轮完成了什么
- 修改了哪些文件
- 如何配置 DeepSeek
- 如何启动和测试
- 验证结果
- 当前风险和需要确认事项
- 下一阶段建议
```

## 十一、归档标签

- AI Agent Web 应用
- 产品评审 Agent
- Spring Boot 3
- MyBatis Plus
- MySQL
- Mock Agent
- DeepSeek 待接入
- Demo
