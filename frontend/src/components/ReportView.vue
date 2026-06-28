<template>
  <article class="report-view">
    <header class="report-cover">
      <div class="report-cover__copy">
        <div class="report-stamps">
          <span>产品决策区</span>
          <span>{{ modeLabels[review.mode] }}</span>
          <span class="decision-stamp" :class="`decision-stamp--${goDecision.toLowerCase()}`">{{ goDecisionLabel }}</span>
        </div>
        <p class="eyebrow">PM 评审纪要 / {{ review.createdAt }}</p>
        <h1>{{ review.oneLineVerdict }}</h1>
        <p class="go-reason">{{ goDecisionReason }}</p>

        <div v-if="review.status === 'SUCCESS'" class="report-metrics">
          <div class="risk-panel" :style="{ '--risk': `${boundedBeatScore}%` }">
            <span>毒打指数</span>
            <strong>{{ review.beatScore }}/100</strong>
            <div class="risk-track"><i></i></div>
          </div>
          <div>
            <span>产品定位评分</span>
            <strong>{{ review.positioningScore }}/100</strong>
          </div>
        </div>

        <p v-if="successMetric" class="success-metric report-success-metric">
          <strong>成功指标：</strong>{{ successMetric }}
        </p>
      </div>
      <PixelPig size="large" />
    </header>

    <div v-if="review.status === 'FAILED'" class="failed-state">
      <CircleAlert :size="24" />
      <div>
        <h3>评审失败</h3>
        <p>{{ review.errorMessage || 'AI 评审服务暂时不可用，请稍后重试。' }}</p>
      </div>
    </div>

    <template v-else>
      <div class="report-layout">
        <aside class="report-toc" aria-label="报告目录">
          <p>报告目录</p>
          <a v-for="section in sections" :key="section.id" :href="`#${section.id}`">
            <span>{{ section.en }}</span>
            <strong>{{ section.zh }}</strong>
          </a>
        </aside>

        <div class="report-body">
          <section id="pain" class="report-section report-section--lead">
            <div class="section-title">
              <span>01</span>
              <div>
                <p>痛点是否真实</p>
                <h2>用户痛点分析</h2>
              </div>
            </div>
            <p class="redline">{{ report.painPointAnalysis }}</p>
            <span class="annotation-note">想清楚“谁在什么场景下非用不可”。</span>
          </section>

          <section id="fake-risk" class="report-section">
            <div class="section-title">
              <span>02</span>
              <div>
                <p>哪些可能没人真用</p>
                <h2>伪需求风险</h2>
              </div>
            </div>
            <ListBlock title="重点风险" :items="report.fakeDemandRisks" />
          </section>

          <section id="redundancy" class="report-section">
            <div class="section-title">
              <span>03</span>
              <div>
                <p>第一版先别做什么</p>
                <h2>功能冗余检查</h2>
              </div>
            </div>
            <ListBlock title="建议砍掉或延后" :items="report.featureRedundancyCheck" />
          </section>

          <section id="cold-start" class="report-section">
            <div class="section-title">
              <span>04</span>
              <div>
                <p>第一批用户从哪里来</p>
                <h2>冷启动问题</h2>
              </div>
            </div>
            <ListBlock title="上线前要面对的问题" :items="report.coldStartProblems" />
          </section>

          <section id="mvp" class="report-section note-section">
            <div class="section-title">
              <span>05</span>
              <div>
                <p>先把产品砍小</p>
                <h2>最小可行性方案</h2>
              </div>
            </div>
            <ListBlock title="先做这些" :items="report.mvpSuggestions" />
          </section>

          <section id="minimum" class="report-section note-section">
            <div class="section-title">
              <span>06</span>
              <div>
                <p>能开发、能验收的版本</p>
                <h2>最小可开发版本</h2>
              </div>
            </div>
            <p>{{ report.minimumBuildVersion.goal }}</p>
            <div class="two-column-list">
              <ListBlock title="核心功能" :items="report.minimumBuildVersion.coreFeatures" />
              <ListBlock title="暂不实现" :items="report.minimumBuildVersion.excludedFeatures" />
            </div>
          </section>

          <section id="validation" class="report-section note-section validation-section">
            <div class="section-title">
              <span>07</span>
              <div>
                <p>怎么证明值得继续</p>
                <h2>验证计划</h2>
              </div>
            </div>
            <p v-if="successMetric" class="success-metric">
              <strong>成功指标：</strong>{{ successMetric }}
            </p>
            <ListBlock title="验证动作" :items="validationPlan" />
          </section>

          <section id="prompt" class="report-section prompt-block">
            <div class="section-title">
              <span>08</span>
              <div>
                <p>可直接复制给 AI 编程工具</p>
                <h2>Codex/Cursor 开发 Prompt</h2>
              </div>
              <button class="tiny-copy" type="button" title="复制开发 Prompt" @click="copyPrompt">
                <Copy :size="15" />
                <span>{{ copied === 'prompt' ? '已复制' : '复制' }}</span>
              </button>
            </div>
            <pre>{{ report.developerPrompt }}</pre>
          </section>
        </div>
      </div>

      <footer class="report-toolbar">
        <button class="secondary-button" type="button" @click="downloadMarkdown(review)">
          <Download :size="17" />
          <span>导出 Markdown</span>
        </button>
        <button class="secondary-button" type="button" @click="copyPrompt">
          <ClipboardList :size="17" />
          <span>{{ copied === 'prompt' ? '已复制 Prompt' : '复制开发 Prompt' }}</span>
        </button>
        <button class="secondary-button strong-action" type="button" @click="copyReport">
          <Copy :size="17" />
          <span>{{ copied === 'report' ? '已复制报告' : '复制完整报告' }}</span>
        </button>
        <RouterLink class="secondary-button" to="/">
          <ArrowLeft :size="17" />
          <span>返回工作台</span>
        </RouterLink>
      </footer>
    </template>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowLeft, CircleAlert, ClipboardList, Copy, Download } from '@lucide/vue';

import ListBlock from '@/components/ListBlock.vue';
import PixelPig from '@/components/PixelPig.vue';
import type { ReviewDetailResponse } from '@/types/review';
import { copyText } from '@/utils/clipboard';
import { modeLabels } from '@/utils/labels';
import { downloadMarkdown } from '@/utils/markdownExport';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const copied = ref<'report' | 'prompt' | ''>('');
const report = computed(() => props.review.report);
const boundedBeatScore = computed(() => Math.min(100, Math.max(0, props.review.beatScore || 0)));
const goDecision = computed(() => report.value?.goDecision || 'CONTINUE');
const goDecisionLabel = computed(() => decisionLabels[goDecision.value]);
const goDecisionReason = computed(
  () => report.value?.goDecisionReason || '建议继续验证，但先收缩范围并确认真实用户动机。',
);
const successMetric = computed(() => report.value?.minimumBuildVersion?.successMetric || '');
const validationPlan = computed(() => report.value?.minimumBuildVersion?.validationPlan ?? []);

const decisionLabels = {
  CONTINUE: '建议继续',
  PIVOT: '建议调整方向',
  PAUSE: '建议暂缓',
};

const sections = [
  { id: 'pain', en: '01', zh: '用户痛点分析' },
  { id: 'fake-risk', en: '02', zh: '伪需求风险' },
  { id: 'redundancy', en: '03', zh: '功能冗余检查' },
  { id: 'cold-start', en: '04', zh: '冷启动问题' },
  { id: 'mvp', en: '05', zh: 'MVP 改造建议' },
  { id: 'minimum', en: '06', zh: '最小可开发版本' },
  { id: 'validation', en: '07', zh: '验证计划' },
  { id: 'prompt', en: '08', zh: '开发 Prompt' },
];

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
