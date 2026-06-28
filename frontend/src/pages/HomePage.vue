<template>
  <section class="workbench-shell">
    <div class="workbench-hero">
      <div class="hero-mascot">
        <PixelPig size="large" />
        <span class="stamp-label">Product Review Desk</span>
      </div>

      <div class="hero-copy">
        <p class="eyebrow">AI 产品评审工作台</p>
        <h1>你的想法不一定完美，但值得被认真打磨</h1>
        <p>把一个模糊点子，变成更清楚、更能落地的产品方案。</p>
      </div>
    </div>

    <div class="workbench-main" :class="{ 'workbench-main--with-result': hasReviewWorkspace }">
      <ReviewInputForm class="workbench-input" :loading="store.loading" @submit="handleSubmit" />
      <aside class="latest-docket review-docket" aria-label="评审工作台">
        <template v-if="store.loading">
          <p class="eyebrow">Reviewing</p>
          <div class="desk-loading" aria-live="polite">
            <LoaderCircle class="spin" :size="24" />
            <div>
              <h2>正在批注你的草稿</h2>
              <p>先看痛点，再砍范围，最后整理成报告。</p>
            </div>
          </div>
        </template>

        <template v-else-if="submitError">
          <p class="eyebrow">Need Retry</p>
          <div class="inline-error">
            <CircleAlert :size="20" />
            <span>{{ submitError }}</span>
          </div>
          <button class="secondary-button wide" type="button" :disabled="!lastPayload" @click="retrySubmit">
            <RefreshCw :size="17" />
            <span>重试评审</span>
          </button>
        </template>

        <template v-else-if="store.currentReview">
          <div class="summary-panel">
            <div class="summary-panel__header">
              <p class="eyebrow">Review Summary #{{ store.currentReview.id }}</p>
              <span class="decision-stamp" :class="`decision-stamp--${goDecision.toLowerCase()}`">
                {{ goDecisionLabel }}
              </span>
            </div>
            <h2>{{ store.currentReview.oneLineVerdict }}</h2>
            <p class="decision-reason">{{ goDecisionReason }}</p>

            <div class="mini-scores">
              <span>毒打 {{ store.currentReview.beatScore }}/100</span>
              <span>定位 {{ store.currentReview.positioningScore }}/100</span>
              <span v-if="providerBadge">{{ providerBadge }}</span>
            </div>

            <div class="summary-notes">
              <div>
                <strong>成功指标</strong>
                <p>{{ successMetric }}</p>
              </div>
              <div>
                <strong>下一步验证</strong>
                <ul class="summary-checklist">
                  <li v-for="item in keyValidationPlan" :key="item">{{ item }}</li>
                </ul>
              </div>
            </div>

            <div class="summary-actions">
              <RouterLink
                class="secondary-button wide strong-action"
                :to="{ name: 'review-detail', params: { id: store.currentReview.id } }"
              >
                <FileText :size="17" />
                <span>查看完整报告</span>
              </RouterLink>
              <button class="secondary-button wide" type="button" @click="copyDeveloperPrompt">
                <ClipboardList :size="17" />
                <span>{{ promptCopied ? '已复制 Prompt' : '复制开发 Prompt' }}</span>
              </button>
              <button class="secondary-button wide" type="button" @click="resetReview">
                <RotateCcw :size="17" />
                <span>重新评审</span>
              </button>
            </div>
          </div>
        </template>

        <template v-else>
          <p class="eyebrow">Review Queue</p>
          <h2>等待第一份产品评审</h2>
          <p>提交后这里会显示进度、错误或报告摘要；历史记录会自动保留。</p>
        </template>
      </aside>
    </div>

    <section class="capability-index" aria-label="能力说明">
      <article>
        <span>01</span>
        <h3>伪需求洞察</h3>
        <p>从真实痛点、使用场景和付费动机里挑出薄弱处。</p>
      </article>
      <article>
        <span>02</span>
        <h3>落地建议</h3>
        <p>把泛泛的想法收敛成更小、更能验证的 MVP。</p>
      </article>
      <article>
        <span>03</span>
        <h3>多角度输入</h3>
        <p>支持导师、毒舌 PM、甲方视角三种评审语气。</p>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { CircleAlert, ClipboardList, FileText, LoaderCircle, RefreshCw, RotateCcw } from '@lucide/vue';

import PixelPig from '@/components/PixelPig.vue';
import ReviewInputForm from '@/components/ReviewInputForm.vue';
import { useReviewStore } from '@/stores/reviewStore';
import type { CreateReviewPayload } from '@/types/review';
import { copyText } from '@/utils/clipboard';

const store = useReviewStore();
const submitError = ref('');
const lastPayload = ref<CreateReviewPayload | null>(null);
const promptCopied = ref(false);

const hasReviewWorkspace = computed(() => store.loading || Boolean(submitError.value) || Boolean(store.currentReview));
const goDecision = computed(() => store.currentReview?.report?.goDecision || 'CONTINUE');
const goDecisionLabel = computed(() => decisionLabels[goDecision.value]);
const goDecisionReason = computed(
  () => store.currentReview?.report?.goDecisionReason || '建议继续验证，但先收缩范围并确认真实用户动机。',
);
const successMetric = computed(
  () => store.currentReview?.report?.minimumBuildVersion?.successMetric || '先用小样本验证用户是否愿意完成提交并复用报告。',
);
const providerBadge = computed(() => {
  const review = store.currentReview;
  if (!review?.providerName) {
    return '';
  }
  if (review.fallbackUsed) {
    return `Fallback: ${review.providerName}`;
  }
  return review.providerName === 'mock' ? 'Mock 评审' : review.providerName;
});
const keyValidationPlan = computed(() => {
  const plan = store.currentReview?.report?.minimumBuildVersion?.validationPlan?.filter(Boolean) ?? [];
  return plan.length > 0 ? plan.slice(0, 2) : ['找 3-5 个目标用户提交真实想法。', '观察他们是否愿意保存报告或复制开发 Prompt。'];
});

const decisionLabels = {
  CONTINUE: '建议继续',
  PIVOT: '建议调整方向',
  PAUSE: '建议暂缓',
};

async function handleSubmit(payload: CreateReviewPayload) {
  lastPayload.value = payload;
  submitError.value = '';
  promptCopied.value = false;
  try {
    await store.create(payload);
  } catch {
    submitError.value = store.error || '提供评审服务时暂时遇到问题，请稍后重试。';
  }
}

async function retrySubmit() {
  if (!lastPayload.value) {
    return;
  }
  await handleSubmit(lastPayload.value);
}

async function copyDeveloperPrompt() {
  const prompt = store.currentReview?.report?.developerPrompt || '';
  if (!prompt) {
    return;
  }
  await copyText(prompt);
  promptCopied.value = true;
  window.setTimeout(() => {
    promptCopied.value = false;
  }, 1600);
}

function resetReview() {
  store.currentReview = null;
  store.error = '';
  submitError.value = '';
  promptCopied.value = false;
}

</script>
