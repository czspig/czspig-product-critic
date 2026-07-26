# Codex 任务：实现二期第一步“项目工作台 MVP”

## 一、当前项目上下文

- 项目名称：`czspig-product-critic`，中文名“猪猪产品毒舌官”。
- 项目类型：Vue 3 + Spring Boot 全栈 AI 产品评审应用。
- 前端技术栈：Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Lucide Vue。
- 后端技术栈：Spring Boot 3、Java 17、MyBatis Plus、MySQL。
- AI：现有 DeepSeek Provider + Mock Provider，不在本轮调整。
- 当前阶段：一期评审闭环已完成，开始二期“产品验证与决策工作台”开发。
- 已完成能力：创建结构化产品评审、历史记录、评审详情、`ideaGroupId` 版本迭代、Go/Pivot/Pause 决策、多维评分、关键假设、未知问题、验证计划、Markdown/任务包导出。
- 当前身份机制：通过 `X-Session-Id` 隔离匿名数据，尚未实现正式登录和支付。
- 本轮任务类型：功能开发类，同时包含少量项目架构整理。

产品方向不是继续增加一次性报告，而是让独立开发者在一个项目中持续查看：当前结论是什么、结论依据是什么、还缺什么信息、下一步要验证什么、不同版本发生了什么变化。

## 二、本轮目标

请在当前仓库中实现“项目工作台 MVP”：

1. 暂时复用现有 `ideaGroupId` 作为项目标识，不新增正式项目表，不迁移数据库。
2. 增加一个面向项目工作台的后端聚合接口，复用现有评审和版本数据。
3. 增加 `/projects/:groupId` 前端页面，展示当前决策、关键假设、未知问题、验证计划、质量状态和版本时间线。
4. 从现有评审详情页和迭代版本页提供清晰的“进入项目工作台”入口。
5. 补充必要的后端测试、前端类型/API/状态管理和 API 文档。

本轮不要处理：

- 不接入联网搜索、竞品数据或行业报告。
- 不新增证据、来源、调研任务、项目、订阅或支付数据库表。
- 不实现登录、JWT、OAuth 或用户中心。
- 不实现聊天窗口、RAG、PPT/Word 生成和市场研究 Agent。
- 不调整 DeepSeek Prompt、Provider、输出 Parser 或 AI 质量规则。
- 不删除或替换现有历史页、评审详情页和迭代版本页。
- 不进行全局视觉重构。

## 三、开始前必须检查

先读取并分析真实项目，不要直接写代码。至少检查：

1. `README.md`
2. `docs/product-architecture.md`
3. `docs/phase2-development-plan.md`
4. `docs/api.md`
5. `docs/database-schema.md`
6. `db/schema.sql`
7. `backend/pom.xml`
8. `frontend/package.json`
9. `ReviewController`、`ReviewService`、`ReviewServiceImpl`
10. `ReviewGroupResponse`、`ReviewDetailResponse`、`ReviewRecordEntity` 和相关 Mapper
11. 后端现有评审服务测试
12. 前端 `reviewApi.ts`、`reviewStore.ts`、`review.ts`、路由文件
13. `ReviewResultPage.vue`、`ReportView.vue`、`IdeaVersionsPage.vue`、`HistoryPage.vue`
14. 全局样式和现有页面布局组件

开始修改前：

- 先运行 `git status --short`，识别用户已有的未提交改动。
- 当前已知下列 AI 相关文件可能存在用户未提交改动，必须保留且本轮原则上不要修改：
  - `backend/src/main/java/com/czspig/productcritic/ai/DeepSeekAiProvider.java`
  - `backend/src/main/java/com/czspig/productcritic/ai/ProductReviewOutputParser.java`
  - `backend/src/main/java/com/czspig/productcritic/ai/ProductReviewPromptBuilder.java`
  - `backend/src/test/java/com/czspig/productcritic/ai/ProductReviewOutputParserTest.java`
  - `backend/src/test/java/com/czspig/productcritic/ai/DeepSeekAiProviderTest.java`
- 不要回滚、覆盖、格式化或顺手整理这些用户改动。
- 如果本提示词描述与实际代码冲突，以实际代码为准，并在实施计划和最终总结中说明差异。

请先向我简要说明：

- 你识别到的现有评审、版本和 session 隔离链路。
- 项目工作台聚合数据应从哪些现有方法和字段复用。
- 计划修改和新增哪些文件。

说明后继续实施，不需要仅因为普通实现细节再次等待确认；只有发现会破坏现有数据、需要改变本轮范围或必须修改上述 AI 文件时才暂停询问。

## 四、后端任务

### 4.1 设计项目工作台响应

新增清晰的项目工作台 DTO。字段命名可根据现有 DTO 风格调整，但至少提供：

- 项目标识：`ideaGroupId` 或语义更清晰的 `projectId`，值仍来自 `ideaGroupId`。
- 项目标题或摘要：从现有评审输入摘要稳定派生，不调用 AI。
- 当前最新评审 ID、版本号、更新时间和总版本数。
- 当前 Go/Pivot/Pause 决策及原因。
- 当前综合评分、定位评分或现有最有意义的评分摘要。
- 当前质量状态或置信度摘要。
- 当前关键假设列表。
- 当前未知问题列表。
- 当前成功指标和验证计划。
- 版本时间线，优先复用现有版本响应结构，避免复制解析逻辑。

不要把整个 `report_json` 原样透传给前端后再自行猜字段。聚合逻辑应在后端明确完成，并兼容旧记录中的可选字段和空数组。

### 4.2 增加查询接口

建议接口：

```http
GET /api/projects/{ideaGroupId}
X-Session-Id: <session-id>
```

要求：

- 必须沿用现有 `X-Session-Id` 解析和隔离逻辑。
- 不得返回其他 session 的项目数据。
- 项目不存在或不属于当前 session 时，沿用现有 `NOT_FOUND` 业务错误。
- 最新版本必须按明确规则确定，不依赖数据库默认顺序。
- 旧数据 `ideaGroupId` 为空时，继续遵循现有兼容规则，不要制造第二套规则。
- 尽量复用现有 `ReviewService` 和版本解析能力；只有确有必要才新增小型私有映射方法。

### 4.3 后端测试

在现有测试风格内覆盖至少这些场景：

1. 正常返回一个包含多个版本的项目工作台。
2. 正确选择最新版本作为当前决策来源。
3. 关键假设、未知问题或验证计划为空时返回稳定空集合，而不是触发空指针。
4. 项目不存在或 session 不匹配时返回现有业务异常语义。
5. 不破坏现有评审详情和版本查询测试。

不要为本轮引入新的测试框架。

## 五、前端任务

### 5.1 类型、API 和状态

- 在现有类型文件中新增项目工作台响应类型，避免使用 `any`。
- 新增项目工作台 API 调用，遵循现有 `http` 和 `unwrap` 封装。
- 在现有 Pinia store 中增加必要状态和加载方法，或者按项目当前风格新增范围很小的 store；请先说明选择理由。
- 正确处理加载、失败、空数据和路由参数非法状态。

### 5.2 页面与路由

新增路由：

```text
/projects/:groupId
```

项目工作台页面必须首先服务于重复使用和快速扫描，不做营销落地页。建议结构：

1. 顶部项目标题、版本数、最后更新时间和当前决策状态。
2. 当前决策区：一句话结论、决策原因、主要评分和质量状态。
3. “关键假设”与“仍需确认”区域，明确区分已知事实、推断和未知问题。
4. 验证计划：成功指标、下一步验证事项和可执行顺序。
5. 版本时间线：可以进入任意历史评审或现有版本对比页。
6. 操作入口：查看最新完整报告、继续创建下一版本、查看版本对比。

交互要求：

- 优先复用现有按钮、状态标签、布局变量和 Lucide 图标。
- 不新增卡片套卡片，不做大面积营销 Hero。
- 保持桌面和移动端文字不溢出、不重叠。
- 固定功能区域的尺寸和响应式约束，避免动态内容导致明显布局跳动。
- 空列表必须显示清晰的空状态，不显示伪造占位内容。
- 不在页面里加入解释“二期功能规划”的宣传文案。

### 5.3 现有页面入口

- 在评审结果页或 `ReportView` 的操作区增加“项目工作台”入口。
- 在 `IdeaVersionsPage` 增加返回项目工作台的入口。
- 保留现有“查看迭代版本”入口及行为。
- 如果历史页存在重复版本记录，不在本轮重构为项目列表；可记录为后续建议。

## 六、文档要求

更新 `docs/api.md`，记录：

- 新接口地址和请求头。
- 响应字段示例。
- session 隔离和错误语义。
- 当前 `ideaGroupId` 暂时承担项目标识，后续会迁移到正式项目实体的边界说明。

如果实现与 `docs/phase2-development-plan.md` 的阶段 1 范围有必要差异，只做最小文档修正并说明原因，不要扩写后续阶段实现细节。

## 七、修改约束

- 保持现有项目结构、命名、API 响应包装和代码风格。
- 优先复用已有 DTO、服务方法、组件和样式变量。
- 不进行无关重构，不修改与本任务无关的文件。
- 不删除用户已有功能，不改变现有公开接口行为。
- 不提交 API Key、数据库密码或真实用户数据。
- 日志不要记录完整用户输入、完整 Prompt、Authorization 或 session 敏感值。
- 如需新增依赖，必须先说明必要性；本任务原则上不应需要新增依赖。
- 不自行创建 Git 提交，除非我在新对话中明确要求。

## 八、验证要求

完成后请尽可能执行：

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
pnpm build
```

如果 `pnpm` 不可用，可使用项目环境中可用的等价命令，但要说明替代方式。

还要进行最小手动验证：

1. 使用 Mock Provider 创建 V1 评审。
2. 基于 V1 创建 V2。
3. 从评审结果进入项目工作台。
4. 确认工作台显示 V2 的当前决策和两条版本记录。
5. 从工作台进入最新报告和版本对比。
6. 使用另一个 `X-Session-Id` 请求同一项目，确认无法访问。
7. 在桌面与移动宽度下检查页面没有文字溢出或操作区重叠。

如果数据库、端口、API Key 或本地环境导致某项无法执行，说明具体原因，不要把未执行写成通过。Mock 成功不能写成真实 DeepSeek 验收成功。

## 九、最终输出要求

完成后请用中文输出：

1. 本轮完成了什么。
2. 修改和新增了哪些文件，以及每个文件的目的。
3. 项目工作台的数据聚合规则。
4. 新增接口和页面入口。
5. 运行了哪些测试、构建和手动验证，结果如何。
6. 哪些验证没有执行及原因。
7. 仍然存在的问题、兼容风险和安全边界。
8. 用户需要手动完成的事项。
9. 下一步建议，但不要在本轮继续实现阶段 2 的证据模型。
