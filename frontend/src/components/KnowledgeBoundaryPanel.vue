<template>
  <section class="boundary-panel" aria-label="AI 判断边界">
    <div class="boundary-panel__header">
      <div>
        <p class="eyebrow">Evidence Boundary</p>
        <h2>AI 的判断边界</h2>
      </div>
      <span>{{ totalItems }} 条依据</span>
    </div>

    <div v-if="!boundary" class="legacy-empty">
      该报告生成于可信评审功能上线前，暂无事实、推测、假设和未知问题分层。
    </div>

    <div v-else class="boundary-grid">
      <section class="boundary-column">
        <h3>已提供事实</h3>
        <article v-for="fact in boundary.providedFacts" :key="fact.statement" class="boundary-item">
          <strong>{{ fact.statement }}</strong>
          <small v-if="fact.sourceText">原文：{{ fact.sourceText }}</small>
        </article>
        <p v-if="isEmpty(boundary.providedFacts)" class="column-empty">暂无可定位事实</p>
      </section>

      <section class="boundary-column">
        <h3>AI 推测</h3>
        <article v-for="item in boundary.inferences" :key="item.statement" class="boundary-item">
          <strong>{{ item.statement }}</strong>
          <small>{{ confidenceLabel(item.confidence) }} · {{ item.basis || '基于当前输入推断' }}</small>
        </article>
        <p v-if="isEmpty(boundary.inferences)" class="column-empty">暂无推测</p>
      </section>

      <section class="boundary-column">
        <h3>关键假设</h3>
        <article v-for="item in boundary.criticalHypotheses" :key="item.statement" class="boundary-item boundary-item--hypothesis">
          <strong>{{ item.statement }}</strong>
          <small>{{ importanceLabel(item.importance) }} · {{ item.impactIfFalse || '如果不成立，需要调整方向' }}</small>
          <em v-if="item.relatedDimensions?.length">{{ item.relatedDimensions.join(' / ') }}</em>
        </article>
        <p v-if="isEmpty(boundary.criticalHypotheses)" class="column-empty">暂无关键假设</p>
      </section>

      <section class="boundary-column">
        <h3>当前未知</h3>
        <article v-for="item in boundary.unknowns" :key="item.question" class="boundary-item">
          <strong>{{ item.question }}</strong>
          <small>{{ item.reason || '需要用访谈或小实验补证据' }}</small>
        </article>
        <p v-if="isEmpty(boundary.unknowns)" class="column-empty">暂无未知问题</p>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { ReviewDetailResponse } from '@/types/review';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const boundary = computed(() => props.review.report?.knowledgeBoundary);
const totalItems = computed(() => {
  if (!boundary.value) return 0;
  return (boundary.value.providedFacts?.length ?? 0)
    + (boundary.value.inferences?.length ?? 0)
    + (boundary.value.criticalHypotheses?.length ?? 0)
    + (boundary.value.unknowns?.length ?? 0);
});

function isEmpty(items?: unknown[]) {
  return !items || items.length === 0;
}

function confidenceLabel(value?: string) {
  if (value === 'HIGH') return '高置信';
  if (value === 'MEDIUM') return '中置信';
  return '低置信';
}

function importanceLabel(value?: string) {
  if (value === 'HIGH') return '高优先级';
  if (value === 'LOW') return '低优先级';
  return '中优先级';
}
</script>

<style scoped>
.boundary-panel {
  display: grid;
  gap: 16px;
  border: 2px solid var(--border-dark);
  padding: 18px;
  background: var(--paper);
  box-shadow: 5px 5px 0 rgba(38, 35, 31, 0.16);
}

.boundary-panel__header {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  justify-content: space-between;
  gap: 10px;
}

.boundary-panel__header h2 {
  margin-top: 4px;
}

.boundary-panel__header > span,
.legacy-empty,
.column-empty {
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.legacy-empty,
.column-empty {
  border: 1px dashed rgba(113, 103, 91, 0.45);
  padding: 12px;
  background: rgba(242, 232, 215, 0.34);
}

.boundary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.boundary-column {
  display: grid;
  align-content: start;
  gap: 8px;
}

.boundary-column h3 {
  color: var(--ink);
  font-size: 14px;
}

.boundary-item {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(113, 103, 91, 0.34);
  padding: 10px;
  background: rgba(242, 232, 215, 0.34);
}

.boundary-item--hypothesis {
  border-color: rgba(166, 64, 45, 0.32);
  background: rgba(239, 216, 204, 0.36);
}

.boundary-item strong {
  color: var(--ink);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.boundary-item small,
.boundary-item em {
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 760;
  overflow-wrap: anywhere;
}

@media (max-width: 980px) {
  .boundary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .boundary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
