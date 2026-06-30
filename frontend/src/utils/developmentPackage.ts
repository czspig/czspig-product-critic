import type { ReviewDetailResponse } from '@/types/review';

function list(values: string[] | undefined) {
  const items = values?.filter(Boolean) ?? [];
  return items.length > 0 ? items.map((item) => `- ${item}`).join('\n') : '- 暂无';
}

export function buildNextDraft(review: ReviewDetailResponse) {
  const minimum = review.report?.minimumBuildVersion;
  return `我想把这个产品想法收缩成一个更小、更容易验证的 MVP。

目标：
${minimum?.goal || review.oneLineVerdict || '先明确一个可验证的产品目标。'}

第一版核心功能：
${list(minimum?.coreFeatures)}

暂时不做：
${list(minimum?.excludedFeatures)}

本轮重点验证：
${list(minimum?.validationPlan)}

成功指标：
${minimum?.successMetric || '让一小批目标用户完成核心任务，并判断是否愿意继续使用。'}`;
}

export function buildDevelopmentPackage(review: ReviewDetailResponse) {
  const report = review.report;
  const minimum = report.minimumBuildVersion;
  return `# 项目开发任务包

## 1. 项目目标

${minimum.goal || '围绕当前产品想法实现最小可运行版本。'}

## 2. 目标用户与痛点

${report.painPointAnalysis || '需要补充目标用户与痛点。'}

## 3. MVP 功能范围

${list(minimum.coreFeatures)}

## 4. 暂不做范围

${list(minimum.excludedFeatures)}

## 5. 成功指标

${minimum.successMetric || '核心功能可运行，用户能完成主要任务。'}

## 6. 验证计划

${list(minimum.validationPlan)}

## 7. 页面建议

根据当前产品目标，设计最小可用页面。

## 8. 后端/API 建议

根据 MVP 功能范围设计必要接口。

## 9. 验收标准

- 核心功能可运行
- 输入输出链路完整
- 用户能完成主要任务
- 不做暂不做范围中的功能

## 10. 给 Codex/Cursor 的开发 Prompt

${report.developerPrompt || '请先补充开发 Prompt。'}
`;
}

export function downloadTextFile(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
