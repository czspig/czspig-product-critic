<template>
  <section class="quality-panel" aria-label="报告质量自检">
    <div class="quality-panel__header">
      <div>
        <p class="eyebrow">Quality Audit</p>
        <h2>报告质量自检</h2>
      </div>
      <span v-if="audit" :class="`status-pill status-pill--${status.toLowerCase()}`">{{ statusLabel(status) }}</span>
    </div>

    <div v-if="!audit" class="legacy-empty">
      该报告生成于可信评审功能上线前，暂无质量自检结果。
    </div>

    <template v-else>
      <p class="quality-summary">{{ audit.summary || '后端已完成基础结构检查。' }}</p>
      <p class="quality-note">这是报告结构、证据边界和可执行性的检查，不代表产品 idea 本身优秀。</p>

      <div class="quality-counts">
        <span class="count count--pass">通过 {{ counts.PASS }}</span>
        <span class="count count--warning">需注意 {{ counts.WARNING }}</span>
        <span class="count count--fail">未通过 {{ counts.FAIL }}</span>
      </div>

      <div class="quality-grid">
        <article v-for="check in checks" :key="check.key" class="quality-check">
          <span :class="`dot dot--${check.status.toLowerCase()}`"></span>
          <div>
            <strong>{{ check.name }}</strong>
            <p>{{ check.message }}</p>
          </div>
        </article>
      </div>

      <div v-if="audit.issues?.length" class="quality-issues">
        <h3>需要注意</h3>
        <ul>
          <li v-for="issue in audit.issues" :key="`${issue.type}-${issue.message}`">
            <strong>{{ severityLabel(issue.severity) }}</strong>
            <span>{{ issue.message }}</span>
            <small v-if="issue.suggestion">{{ issue.suggestion }}</small>
          </li>
        </ul>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { QualityCheck, QualityStatus, ReviewDetailResponse } from '@/types/review';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const audit = computed(() => props.review.report?.qualityAudit);
const status = computed<QualityStatus>(() => audit.value?.overallStatus || 'WARNING');
const checks = computed<QualityCheck[]>(() => audit.value?.checks ?? []);
const counts = computed(() => checks.value.reduce(
  (result, check) => {
    result[check.status] += 1;
    return result;
  },
  { PASS: 0, WARNING: 0, FAIL: 0 } as Record<QualityStatus, number>,
));

function statusLabel(value: QualityStatus) {
  if (value === 'PASS') return '通过';
  if (value === 'FAIL') return '未通过';
  return '需注意';
}

function severityLabel(value?: string) {
  if (value === 'HIGH') return '高';
  if (value === 'LOW') return '低';
  return '中';
}
</script>

<style scoped>
.quality-panel {
  display: grid;
  gap: 16px;
  border: 2px solid var(--border-dark);
  padding: 18px;
  background: var(--paper);
  box-shadow: 5px 5px 0 rgba(38, 35, 31, 0.16);
}

.quality-panel__header {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  justify-content: space-between;
  gap: 10px;
}

.quality-panel__header h2 {
  margin-top: 4px;
}

.status-pill,
.count {
  border: 1px solid rgba(38, 35, 31, 0.22);
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 900;
}

.status-pill--pass,
.count--pass {
  background: rgba(219, 231, 223, 0.72);
  color: #2f5d52;
}

.status-pill--warning,
.count--warning {
  background: rgba(255, 244, 216, 0.82);
  color: #8a5a15;
}

.status-pill--fail,
.count--fail {
  background: rgba(239, 216, 204, 0.72);
  color: #9d2d22;
}

.legacy-empty,
.quality-summary,
.quality-note {
  color: var(--muted);
  font-size: 13px;
  font-weight: 850;
}

.quality-note {
  border-left: 4px solid #d5a23d;
  padding-left: 10px;
}

.legacy-empty {
  border: 1px dashed rgba(113, 103, 91, 0.45);
  padding: 12px;
  background: rgba(242, 232, 215, 0.34);
}

.quality-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quality-check {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 9px;
  border: 1px solid rgba(113, 103, 91, 0.34);
  padding: 10px;
  background: rgba(242, 232, 215, 0.34);
}

.dot {
  width: 10px;
  height: 10px;
  margin-top: 4px;
  border: 1px solid var(--border-dark);
}

.dot--pass {
  background: #47695f;
}

.dot--warning {
  background: #d5a23d;
}

.dot--fail {
  background: #a6402d;
}

.quality-check strong,
.quality-issues h3 {
  color: var(--ink);
  font-size: 13px;
}

.quality-check p {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 760;
}

.quality-issues {
  display: grid;
  gap: 8px;
  border-top: 1px solid rgba(113, 103, 91, 0.28);
  padding-top: 12px;
}

.quality-issues ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.quality-issues li {
  display: grid;
  gap: 3px;
  border-left: 4px solid #a6402d;
  padding-left: 10px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 760;
}

.quality-issues li strong {
  color: #9d2d22;
}

.quality-issues li small {
  color: var(--ink);
}

@media (max-width: 760px) {
  .quality-grid {
    grid-template-columns: 1fr;
  }
}
</style>
