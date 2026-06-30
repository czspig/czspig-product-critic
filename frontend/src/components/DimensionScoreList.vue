<template>
  <section class="dimension-score-list" aria-label="多维评分">
    <div class="dimension-score-list__header">
      <div>
        <p class="eyebrow">Score Map</p>
        <h2>多维产品评分</h2>
      </div>
      <span>{{ strongestLabel }} 强，{{ weakestLabel }} 弱</span>
    </div>

    <div class="dimension-grid">
      <article v-for="item in dimensions" :key="item.label" class="dimension-item">
        <div class="dimension-item__top">
          <strong>{{ item.label }}</strong>
          <span>{{ item.score }}/100</span>
        </div>
        <div class="dimension-track" :aria-label="`${item.label} ${item.score} 分`">
          <i :style="{ width: `${item.score}%`, background: item.color }"></i>
        </div>
        <p>{{ item.note }}</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { ReviewDetailResponse } from '@/types/review';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

interface DimensionItem {
  label: string;
  score: number;
  note: string;
  color: string;
}

const report = computed(() => props.review.report);
const beatScore = computed(() => clamp(props.review.beatScore ?? report.value?.beatScore ?? 50));
const positioningScore = computed(() => clamp(props.review.positioningScore ?? report.value?.positioningScore ?? 50));

const dimensions = computed<DimensionItem[]>(() => {
  const painText = report.value?.painPointAnalysis || '';
  const riskCount = report.value?.fakeDemandRisks?.length ?? 0;
  const validationCount = report.value?.minimumBuildVersion?.validationPlan?.length ?? 0;
  const hasBusiness = containsAny(`${props.review.inputContent} ${painText}`, ['商业模式', '收费', '付费', '订阅', '收入', '合作']);
  const hasMarket = containsAny(`${props.review.inputContent} ${painText}`, ['市场', '校园', '求职', '宠物', '企业', '用户', '客户']);

  const pain = clamp(positioningScore.value + (painText.length > 80 ? 8 : -4));
  const market = clamp(positioningScore.value + (hasMarket ? 6 : -8) - Math.max(0, riskCount - 2) * 3);
  const business = clamp(positioningScore.value + (hasBusiness ? 8 : -12));
  const difficulty = clamp(100 - beatScore.value + (validationCount >= 3 ? 6 : 0));
  const growth = clamp((market + positioningScore.value) / 2 + (validationCount >= 3 ? 5 : -3));
  const risk = clamp(100 - beatScore.value);

  return [
    { label: '用户痛点', score: pain, note: pain >= 70 ? '痛点描述比较具体' : '还需要更真实的痛点证据', color: '#a6402d' },
    { label: '市场空间', score: market, note: market >= 70 ? '有可切入的人群或场景' : '市场边界还偏模糊', color: '#47695f' },
    { label: '商业模式', score: business, note: business >= 70 ? '已有初步变现假设' : '付费理由还需要补强', color: '#9b6a2b' },
    { label: '实现难度', score: difficulty, note: difficulty >= 70 ? '首版范围相对可控' : '实现范围或风险偏重', color: '#6e706a' },
    { label: '增长潜力', score: growth, note: growth >= 70 ? '有继续放大的可能' : '冷启动和复用路径待验证', color: '#2f5d52' },
    { label: '风险等级', score: risk, note: risk >= 70 ? '当前风险可控' : '风险偏高，先验证再开工', color: '#9d2d22' },
  ];
});

const strongestLabel = computed(() => [...dimensions.value].sort((a, b) => b.score - a.score)[0]?.label || '痛点');
const weakestLabel = computed(() => [...dimensions.value].sort((a, b) => a.score - b.score)[0]?.label || '风险');

function clamp(value: number) {
  return Math.round(Math.max(0, Math.min(100, value)));
}

function containsAny(content: string, keywords: string[]) {
  return keywords.some((keyword) => content.includes(keyword));
}
</script>

<style scoped>
.dimension-score-list {
  display: grid;
  gap: 16px;
  border: 2px solid var(--border-dark);
  padding: 18px;
  background: var(--paper);
  box-shadow: 5px 5px 0 rgba(38, 35, 31, 0.16);
}

.dimension-score-list__header {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: end;
  justify-content: space-between;
}

.dimension-score-list__header h2 {
  margin-top: 4px;
}

.dimension-score-list__header > span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dimension-item {
  display: grid;
  gap: 8px;
  border: 1px solid rgba(113, 103, 91, 0.34);
  padding: 12px;
  background: rgba(242, 232, 215, 0.34);
}

.dimension-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.dimension-item__top strong {
  color: var(--ink);
}

.dimension-item__top span {
  color: var(--accent-strong);
  font-weight: 900;
}

.dimension-track {
  height: 9px;
  border: 1px solid var(--border-dark);
  background: var(--paper-deep);
}

.dimension-track i {
  display: block;
  height: 100%;
}

.dimension-item p {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 760px) {
  .dimension-grid {
    grid-template-columns: 1fr;
  }
}
</style>
