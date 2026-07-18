<template>
  <section class="score-overview-card" aria-label="一句话结论">
    <div class="overview-head">
      <div>
        <p class="eyebrow">One-line Verdict</p>
        <h2>{{ decisionText }}</h2>
      </div>
      <div class="overview-score">
        <strong v-if="overallScore !== null">{{ overallScore }}/100</strong>
        <span :class="`quality-pill quality-pill--${qualityStatus.toLowerCase()}`">
          {{ qualityStatusLabel }}
        </span>
      </div>
    </div>
    <p class="verdict">{{ verdict }}</p>
    <div class="overview-grid">
      <div>
        <span>优势</span>
        <strong>{{ strongest }}</strong>
      </div>
      <div>
        <span>短板</span>
        <strong>{{ weakest }}</strong>
      </div>
      <div>
        <span>最不确定</span>
        <strong>{{ uncertain }}</strong>
      </div>
    </div>
    <div class="next-step">
      <span>下一步建议</span>
      <strong>{{ nextStep }}</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { OverallDecision, QualityStatus, ReviewDetailResponse } from '@/types/review';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const assessment = computed(() => props.review.report?.overallAssessment);
const overallScore = computed(() => assessment.value?.overallScore ?? null);
const qualityStatus = computed<QualityStatus>(() => props.review.report?.qualityAudit?.overallStatus || 'WARNING');
const qualityStatusLabel = computed(() => {
  if (qualityStatus.value === 'PASS') return '报告质量通过';
  if (qualityStatus.value === 'FAIL') return '报告质量未通过';
  return '报告质量需注意';
});

const decisionText = computed(() => {
  if (assessment.value?.decision) {
    return trustedDecisionLabels[assessment.value.decision];
  }
  const decision = props.review.report?.goDecision;
  if (decision === 'PAUSE') return '不建议投入';
  if (decision === 'PIVOT') return '谨慎验证';
  const beatScore = props.review.beatScore ?? props.review.report?.beatScore ?? 0;
  const positioningScore = props.review.positioningScore ?? props.review.report?.positioningScore ?? 0;
  if (beatScore >= 75 || positioningScore < 45) return '谨慎验证';
  return '值得做';
});

const verdict = computed(() => props.review.oneLineVerdict
  || props.review.report?.oneLineVerdict
  || '这个 idea 还没交代清楚，先别急着开工。');

const strongest = computed(() => assessment.value?.strongestDimension || firstDimension('HIGHER_IS_BETTER', true) || '暂无');
const weakest = computed(() => assessment.value?.weakestDimension || weakestDimension.value || '暂无');
const uncertain = computed(() => assessment.value?.mostUncertainDimension
  || props.review.report?.dimensionScores?.find((item) => item.confidence === 'LOW')?.dimensionName
  || '暂无');

const weakestDimension = computed(() => [...(props.review.report?.dimensionScores ?? [])]
  .sort((a, b) => {
    const left = a.scoreDirection === 'HIGHER_IS_WORSE' ? 100 - a.score : a.score;
    const right = b.scoreDirection === 'HIGHER_IS_WORSE' ? 100 - b.score : b.score;
    return left - right;
  })[0]?.dimensionName);

const nextStep = computed(() => {
  if (assessment.value?.priorityQuestion) {
    return assessment.value.priorityQuestion;
  }
  const minimum = props.review.report?.minimumBuildVersion;
  const firstPlan = minimum?.validationPlan?.find(Boolean);
  if (firstPlan) return firstPlan;
  if (minimum?.goal) return minimum.goal;
  return props.review.report?.goDecisionReason || '先补齐目标用户、使用场景和最小验证动作。';
});

const trustedDecisionLabels: Record<OverallDecision, string> = {
  PROCEED: '值得做',
  VALIDATE_FIRST: '谨慎验证',
  NARROW_SCOPE: '先缩小范围',
  PAUSE: '不建议投入',
};

function firstDimension(direction: string, descending: boolean) {
  const items = [...(props.review.report?.dimensionScores ?? [])].filter((item) => item.scoreDirection === direction);
  items.sort((a, b) => descending ? b.score - a.score : a.score - b.score);
  return items[0]?.dimensionName;
}
</script>

<style scoped>
.score-overview-card {
  display: grid;
  gap: 14px;
  border: 2px solid var(--border-dark);
  padding: 18px;
  background:
    linear-gradient(rgba(255, 244, 216, 0.72), rgba(255, 244, 216, 0.72)),
    var(--paper);
  box-shadow: 5px 5px 0 rgba(38, 35, 31, 0.16);
}

.overview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.overview-head h2 {
  margin-top: 4px;
  color: var(--accent-strong);
  font-size: 28px;
}

.overview-score {
  display: grid;
  justify-items: end;
  gap: 6px;
  min-width: 120px;
}

.overview-score > strong {
  color: var(--ink);
  font-size: 24px;
}

.quality-pill {
  border: 1px solid rgba(38, 35, 31, 0.22);
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 900;
}

.quality-pill--pass {
  background: rgba(219, 231, 223, 0.72);
  color: #2f5d52;
}

.quality-pill--warning {
  background: rgba(255, 244, 216, 0.82);
  color: #8a5a15;
}

.quality-pill--fail {
  background: rgba(239, 216, 204, 0.72);
  color: #9d2d22;
}

.verdict {
  color: #3b342c;
  font-size: 18px;
  font-weight: 900;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.overview-grid > div {
  display: grid;
  gap: 4px;
  border: 1px solid rgba(113, 103, 91, 0.3);
  padding: 10px;
  background: rgba(255, 255, 255, 0.28);
}

.overview-grid span,
.next-step span {
  color: var(--success);
  font-size: 12px;
  font-weight: 900;
}

.overview-grid strong,
.next-step strong {
  color: var(--ink);
}

.next-step {
  display: grid;
  gap: 5px;
  border-left: 4px solid var(--success);
  padding: 10px 0 10px 12px;
  background: rgba(219, 231, 223, 0.36);
}

@media (max-width: 680px) {
  .overview-head {
    display: grid;
  }

  .overview-score {
    justify-items: start;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
