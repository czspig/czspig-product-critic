# 可信评审字段说明

二期维护新增的可信评审结构仍存放在 `review_records.report_json` 内，不新增数据库字段，不影响旧历史记录读取。旧报告如果没有这些字段，前端会显示“暂无评分证据链 / 暂无质量自检结果”的兼容提示。

## report 新增字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `overallAssessment` | object | 可信总分、总体判断、强弱维度、最优先验证问题 |
| `dimensionScores` | array | 6 个固定维度评分：用户痛点、市场空间、商业模式、实现难度、增长潜力、风险等级 |
| `knowledgeBoundary` | object | 用户已提供事实、AI 推测、关键待验证假设、当前未知问题 |
| `qualityAudit` | object | 后端规则自检结果、检查项、质量问题和修复建议 |

## 维度方向

- `painPoint` / `marketSpace` / `businessModel` / `growthPotential`：`HIGHER_IS_BETTER`
- `implementationDifficulty` / `riskLevel`：`HIGHER_IS_WORSE`

## 质量状态

- `PASS`：基础结构、证据链和建议可用
- `WARNING`：报告可用，但存在置信度、边界或一致性风险
- `FAIL`：缺少关键结构或字段非法，需重新生成或修复

## 兼容性

- DeepSeek prompt 和 repair prompt 都要求输出这些字段。
- Mock 输出同样生成这些字段，便于本地演示和测试。
- 历史记录接口会尽量返回质量状态、置信度摘要和关键假设；旧记录缺失时返回空值或空数组。
