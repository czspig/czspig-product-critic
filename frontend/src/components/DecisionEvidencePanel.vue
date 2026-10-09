<template>
  <section id="decision-guide" class="decision-evidence" aria-label="决策与验证摘要">
    <header><div><p class="eyebrow">Decision Guide</p><h2>看完这份报告，下一步做什么？</h2></div><span class="decision-guide__badge">{{ decisionLabel }}</span></header>
    <p class="decision-evidence__reason">{{ reason }}</p>
    <div class="decision-evidence__grid">
      <article v-for="(risk, index) in risks" :key="`${index}-${risk.title}`">
        <h3>风险 {{ index + 1 }} · {{ risk.title }}</h3>
        <p>{{ risk.priorityReason || '优先级沿用原报告；尚缺明确排序依据。' }}</p>
        <p><strong>{{ evidenceLabel(risk.evidenceType) }}依据：</strong>{{ risk.basis || '原报告未提供可定位依据，需要核对原始材料。' }}</p>
        <p><strong>影响：</strong>{{ risk.impact || '影响尚未说明，验证前先明确不成立会怎样。' }}</p>
        <p><strong>验证对象：</strong>{{ risk.target || '与该风险相关的目标用户或责任人' }}</p>
        <ol><li v-for="(step, i) in risk.steps || []" :key="i">{{ step }}</li></ol>
        <p><strong>收集证据：</strong>{{ (risk.evidenceToCollect || []).join('；') || '记录原始行为、任务结果和失败原因' }}</p>
        <p><strong>参考判据：</strong>{{ risk.referenceThreshold || '先与现有办法比较任务收益，未提供数值阈值。' }}</p>
        <p class="decision-evidence__note">{{ risk.thresholdReason || '需在测试前由团队结合样本与预算确认，不能当作行业标准。' }}</p>
      </article>
    </div>
    <div class="decision-evidence__conditions">
      <article><h3>决策条件</h3><ul><li><strong>继续验证：</strong>{{ continueCondition }}</li><li><strong>调整方向：</strong>{{ pivotCondition }}</li><li><strong>暂缓投入：</strong>{{ pauseCondition }}</li></ul></article>
      <p v-if="!guide" class="decision-evidence__note">旧报告缺少逐项验证设计，以上是待完善的建议模板；不能视为完成验证。请补充对象、样本、时间与原始记录。</p>
    </div>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue';
import type { ReviewDetailResponse, RiskExperiment } from '@/types/review';
const props = defineProps<{ review: ReviewDetailResponse }>();
const report = computed(() => props.review.report);
const guide = computed(() => report.value?.validationGuide);
const decisionLabels: Record<string, string> = { CONTINUE: '建议继续验证', PIVOT: '建议调整方向', PAUSE: '建议暂缓投入' };
const decisionLabel = computed(() => guide.value?.informationInsufficient || !guide.value ? '信息不足，先验证' : decisionLabels[report.value?.goDecision || ''] || '信息不足，先验证');
const reason = computed(() => guide.value?.decisionReason || report.value?.goDecisionReason || '当前证据不足，先用低成本验证补齐关键事实，再决定继续、调整或暂停。');
const risks = computed(() => {
  if (guide.value?.risks?.length) return guide.value.risks.slice(0, 3);
  const hypotheses = [...(report.value?.knowledgeBoundary?.criticalHypotheses || [])].sort((a,b) => ({HIGH:0,MEDIUM:1,LOW:2}[a.importance || 'MEDIUM'] - {HIGH:0,MEDIUM:1,LOW:2}[b.importance || 'MEDIUM']));
  const inferred: RiskExperiment[] = hypotheses.slice(0, 3).map(item => ({ title: item.statement, evidenceType: 'HYPOTHESIS', impact: item.impactIfFalse, steps: ['在一周内让目标用户完成与此假设相关的任务，记录不符合假设的行为'], evidenceToCollect: ['任务过程、原始记录和反例'] }));
  if (inferred.length >= 3) return inferred;
  const fallback: RiskExperiment[] = (report.value?.fakeDemandRisks || []).slice(0, 3 - inferred.length).map(item => ({ title: item, evidenceType: 'INFERENCE', basis: '来自旧报告的风险提示，尚未核验', steps: ['把该风险拆成可观察行为，安排一周内的小实验'] }));
  return [...inferred, ...fallback];
});
const continueCondition = computed(() => guide.value?.continueWhen || '目标用户能完成核心任务，并出现可重复的主动使用或明确继续意愿。');
const pivotCondition = computed(() => guide.value?.adjustWhen || '用户任务能完成但价值、场景或目标人群不成立，依据反馈收缩或改换方向。');
const pauseCondition = computed(() => guide.value?.pauseWhen || '核心任务无人愿意完成，或成本与证据长期不匹配；先停止扩展功能。');
function evidenceLabel(type?: string) { return ({FACT:'用户提供事实',INFERENCE:'AI 推断',HYPOTHESIS:'待验证假设'} as Record<string,string>)[type || ''] || '待核对'; }
</script>
<style scoped>
.decision-evidence{display:grid;gap:16px;scroll-margin-top:84px;border:2px solid var(--border-dark);padding:18px;background:var(--paper);box-shadow:5px 5px 0 rgba(38,35,31,.14)}
.decision-evidence header{display:flex;justify-content:space-between;gap:12px;align-items:start}.decision-guide__badge{border:2px solid var(--accent);padding:8px 10px;font-weight:900;color:var(--accent);white-space:nowrap}.decision-evidence__reason{font-size:16px;font-weight:800}.decision-evidence__grid{display:grid;grid-template-columns:1.2fr 1fr 1fr;gap:12px}.decision-evidence article{border-top:1px solid var(--border);padding-top:12px}.decision-evidence h3{font-size:16px}.decision-evidence ol,.decision-evidence ul{display:grid;gap:10px;padding-left:20px}.decision-evidence li{display:grid;gap:3px}.decision-evidence li span,.decision-evidence li small,.decision-evidence__note{color:var(--muted);font-size:12px;font-weight:700}.decision-evidence__note{border-left:3px solid var(--accent);padding-left:8px}@media(max-width:900px){.decision-evidence__grid{grid-template-columns:1fr}}@media(max-width:640px){.decision-evidence header{flex-direction:column}.decision-guide__badge{justify-self:start}}
</style>
