<template>
  <section class="dimension-score-list" aria-label="多维评分证据链">
    <div class="dimension-score-list__header">
      <div>
        <p class="eyebrow">Score Evidence</p>
        <h2>多维评分与证据</h2>
      </div>
      <span v-if="hasTrustedScores">{{ strongestLabel }} 强，{{ weakestLabel }} 待补</span>
    </div>

    <div v-if="!hasTrustedScores" class="legacy-empty">
      该报告生成于可信评审功能上线前，暂无评分证据链。
    </div>

    <div v-else class="dimension-grid">
      <article v-for="item in dimensions" :key="item.dimensionKey" class="dimension-item">
        <button type="button" class="dimension-item__summary" @click="toggle(item.dimensionKey)">
          <span>
            <strong>{{ item.dimensionName }}</strong>
            <small>{{ directionLabel(item.scoreDirection) }}</small>
          </span>
          <b>{{ item.score }}/100</b>
        </button>
        <div class="dimension-track" :aria-label="`${item.dimensionName} ${item.score} 分`">
          <i :style="{ width: `${boundedScore(item.score)}%`, background: colorFor(item) }"></i>
        </div>
        <div class="dimension-meta">
          <span :class="`confidence confidence--${item.confidence?.toLowerCase()}`">
            {{ confidenceLabel(item.confidence) }}
          </span>
          <p>{{ item.summary }}</p>
        </div>

        <div v-if="expanded[item.dimensionKey]" class="dimension-detail">
          <section class="evidence-block">
            <h3>得分依据</h3>
            <ul>
              <li v-for="evidence in nonEmpty(item.evidence)" :key="evidence">{{ evidence }}</li>
            </ul>
          </section>
          <section class="evidence-block">
            <h3>主要扣分项</h3>
            <ul>
              <li v-for="deduction in nonEmpty(item.deductions)" :key="deduction">{{ deduction }}</li>
            </ul>
          </section>
          <section class="evidence-block">
            <h3>缺失信息</h3>
            <ul>
              <li v-for="missing in nonEmpty(item.missingInformation)" :key="missing">{{ missing }}</li>
            </ul>
          </section>
          <section class="evidence-block">
            <h3>提升建议</h3>
            <ul>
              <li v-for="action in nonEmpty(item.improvementActions)" :key="action">{{ action }}</li>
            </ul>
          </section>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue';

import type { DimensionScore, ReviewDetailResponse, ScoreDirection } from '@/types/review';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const expanded = reactive<Record<string, boolean>>({});
const dimensions = computed(() => props.review.report?.dimensionScores ?? []);
const hasTrustedScores = computed(() => dimensions.value.length > 0);
const strongestLabel = computed(() => bestDimension.value?.dimensionName || '痛点');
const weakestLabel = computed(() => weakestDimension.value?.dimensionName || '商业模式');

const bestDimension = computed(() => [...dimensions.value]
  .filter((item) => item.scoreDirection === 'HIGHER_IS_BETTER')
  .sort((a, b) => (b.score ?? 0) - (a.score ?? 0))[0]);

const weakestDimension = computed(() => [...dimensions.value]
  .sort((a, b) => normalizedWeakness(a) - normalizedWeakness(b))[0]);

function normalizedWeakness(item: DimensionScore) {
  const score = item.score ?? 0;
  return item.scoreDirection === 'HIGHER_IS_WORSE' ? 100 - score : score;
}

function toggle(key: string) {
  expanded[key] = !expanded[key];
}

function boundedScore(value?: number) {
  return Math.min(100, Math.max(0, value ?? 0));
}

function confidenceLabel(value?: string) {
  if (value === 'HIGH') return '高置信度';
  if (value === 'MEDIUM') return '中置信度';
  return '低置信度';
}

function directionLabel(value?: ScoreDirection) {
  return value === 'HIGHER_IS_WORSE' ? '分数越高，问题越重' : '分数越高，表现越好';
}

function colorFor(item: DimensionScore) {
  const score = item.score ?? 0;
  if (item.scoreDirection === 'HIGHER_IS_WORSE') {
    return score >= 70 ? '#a6402d' : '#47695f';
  }
  return score >= 70 ? '#47695f' : '#9b6a2b';
}

function nonEmpty(items?: string[]) {
  return items && items.length > 0 ? items : ['暂无'];
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

.dimension-score-list__header > span,
.legacy-empty {
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.legacy-empty {
  border: 1px dashed rgba(113, 103, 91, 0.45);
  padding: 12px;
  background: rgba(242, 232, 215, 0.34);
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dimension-item {
  display: grid;
  gap: 9px;
  border: 1px solid rgba(113, 103, 91, 0.34);
  padding: 12px;
  background: rgba(242, 232, 215, 0.34);
}

.dimension-item__summary {
  display: flex;
  width: 100%;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.dimension-item__summary span {
  display: grid;
  gap: 3px;
}

.dimension-item__summary strong {
  color: var(--ink);
}

.dimension-item__summary small {
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.dimension-item__summary b {
  color: var(--accent-strong);
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

.dimension-meta {
  display: grid;
  gap: 6px;
}

.dimension-meta p {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.confidence {
  width: fit-content;
  border: 1px solid rgba(38, 35, 31, 0.22);
  padding: 3px 7px;
  font-size: 11px;
  font-weight: 900;
}

.confidence--high {
  background: rgba(219, 231, 223, 0.72);
  color: #2f5d52;
}

.confidence--medium {
  background: rgba(255, 244, 216, 0.8);
  color: #8a5a15;
}

.confidence--low {
  background: rgba(239, 216, 204, 0.72);
  color: #9d2d22;
}

.dimension-detail {
  display: grid;
  gap: 10px;
  border-top: 1px solid rgba(113, 103, 91, 0.28);
  padding-top: 10px;
}

.evidence-block {
  display: grid;
  gap: 4px;
}

.evidence-block h3 {
  color: var(--ink);
  font-size: 12px;
}

.evidence-block ul {
  display: grid;
  gap: 4px;
  margin: 0;
  padding-left: 18px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 760;
}

@media (max-width: 760px) {
  .dimension-grid {
    grid-template-columns: 1fr;
  }
}
</style>
