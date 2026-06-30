<template>
  <section class="score-overview-card" aria-label="一句话结论">
    <div>
      <p class="eyebrow">One-line Verdict</p>
      <h2>{{ decisionText }}</h2>
    </div>
    <p class="verdict">{{ verdict }}</p>
    <div class="next-step">
      <span>下一步建议</span>
      <strong>{{ nextStep }}</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { ReviewDetailResponse } from '@/types/review';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const decisionText = computed(() => {
  const decision = props.review.report?.goDecision;
  if (decision === 'PAUSE') {
    return '不建议投入';
  }
  if (decision === 'PIVOT') {
    return '谨慎验证';
  }
  const beatScore = props.review.beatScore ?? props.review.report?.beatScore ?? 0;
  const positioningScore = props.review.positioningScore ?? props.review.report?.positioningScore ?? 0;
  if (beatScore >= 75 || positioningScore < 45) {
    return '谨慎验证';
  }
  return '值得做';
});

const verdict = computed(() => props.review.oneLineVerdict || props.review.report?.oneLineVerdict || '这个 idea 还没交代清楚，先别急着开工。');

const nextStep = computed(() => {
  const minimum = props.review.report?.minimumBuildVersion;
  const firstPlan = minimum?.validationPlan?.find(Boolean);
  if (firstPlan) {
    return firstPlan;
  }
  if (minimum?.goal) {
    return minimum.goal;
  }
  return props.review.report?.goDecisionReason || '先补齐目标用户、使用场景和最小验证动作。';
});
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

.score-overview-card h2 {
  margin-top: 4px;
  color: var(--accent-strong);
  font-size: 28px;
}

.verdict {
  color: #3b342c;
  font-size: 18px;
  font-weight: 900;
}

.next-step {
  display: grid;
  gap: 5px;
  border-left: 4px solid var(--success);
  padding: 10px 0 10px 12px;
  background: rgba(219, 231, 223, 0.36);
}

.next-step span {
  color: var(--success);
  font-size: 12px;
  font-weight: 900;
}

.next-step strong {
  color: var(--ink);
}
</style>
