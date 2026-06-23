<template>
  <article class="report-view">
    <header class="report-header">
      <div>
        <p class="eyebrow">{{ modeLabels[review.mode] }} · {{ review.createdAt }}</p>
        <h2>{{ review.oneLineVerdict }}</h2>
      </div>
      <div v-if="review.status === 'SUCCESS'" class="report-actions">
        <button class="secondary-button" type="button" title="复制完整报告" @click="copyReport">
          <Copy :size="17" />
          <span>{{ copied === 'report' ? '已复制' : '报告' }}</span>
        </button>
        <button class="secondary-button" type="button" title="复制开发 Prompt" @click="copyPrompt">
          <ClipboardList :size="17" />
          <span>{{ copied === 'prompt' ? '已复制' : 'Prompt' }}</span>
        </button>
        <button class="secondary-button" type="button" title="下载 Markdown" @click="downloadMarkdown(review)">
          <Download :size="17" />
          <span>Markdown</span>
        </button>
      </div>
    </header>

    <div v-if="review.status === 'FAILED'" class="failed-state">
      <CircleAlert :size="24" />
      <div>
        <h3>评审失败</h3>
        <p>{{ review.errorMessage || 'AI 评审服务暂时不可用' }}</p>
      </div>
    </div>

    <template v-else>
      <section class="score-grid" aria-label="评分">
        <div class="score-tile">
          <span>毒打指数</span>
          <strong>{{ review.beatScore }}</strong>
        </div>
        <div class="score-tile alt">
          <span>定位评分</span>
          <strong>{{ review.positioningScore }}</strong>
        </div>
        <div class="score-tile compact">
          <span>强度</span>
          <strong>{{ levelLabel }}</strong>
        </div>
      </section>

      <section class="report-section">
        <h3>用户痛点分析</h3>
        <p>{{ report.painPointAnalysis }}</p>
      </section>

      <section class="report-grid">
        <ListBlock title="伪需求风险" :items="report.fakeDemandRisks" />
        <ListBlock title="功能冗余检查" :items="report.featureRedundancyCheck" />
        <ListBlock title="冷启动问题" :items="report.coldStartProblems" />
        <ListBlock title="MVP 改造建议" :items="report.mvpSuggestions" />
      </section>

      <section class="report-section">
        <h3>最小可开发版本</h3>
        <p>{{ report.minimumBuildVersion.goal }}</p>
        <div class="two-column-list">
          <ListBlock title="核心功能" :items="report.minimumBuildVersion.coreFeatures" />
          <ListBlock title="暂不实现" :items="report.minimumBuildVersion.excludedFeatures" />
        </div>
      </section>

      <section class="prompt-block">
        <div class="section-heading">
          <h3>开发 Prompt</h3>
          <button class="icon-button" type="button" title="复制开发 Prompt" @click="copyPrompt">
            <Copy :size="17" />
          </button>
        </div>
        <pre>{{ report.developerPrompt }}</pre>
      </section>
    </template>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { CircleAlert, ClipboardList, Copy, Download } from '@lucide/vue';

import ListBlock from '@/components/ListBlock.vue';
import type { ReviewDetailResponse } from '@/types/review';
import { copyText } from '@/utils/clipboard';
import { modeLabels } from '@/utils/labels';
import { downloadMarkdown } from '@/utils/markdownExport';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const copied = ref<'report' | 'prompt' | ''>('');
const report = computed(() => props.review.report);
const levelLabel = computed(() => ['温和', '直接', '毒舌'][props.review.roastLevel - 1] || props.review.roastLevel);

async function copyReport() {
  await copyText(props.review.reportMarkdown || props.review.oneLineVerdict);
  markCopied('report');
}

async function copyPrompt() {
  await copyText(report.value?.developerPrompt || '');
  markCopied('prompt');
}

function markCopied(type: 'report' | 'prompt') {
  copied.value = type;
  window.setTimeout(() => {
    copied.value = '';
  }, 1600);
}
</script>
