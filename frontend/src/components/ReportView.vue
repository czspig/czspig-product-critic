<template>
  <article class="report-view">
    <nav class="page-toolbar context-navigation" aria-label="报告导航">
      <RouterLink class="secondary-button" :to="returnTarget.path">{{ returnTarget.label }}</RouterLink>
      <RouterLink class="secondary-button" :to="{ name: 'idea-versions', params: { groupId }, query: { from: '/reviews/' + review.id } }">版本历史</RouterLink>
      <RouterLink v-if="returnTarget.path !== '/projects/' + groupId" class="secondary-button" :to="{ name: 'project-workbench', params: { groupId } }">项目工作台</RouterLink>
    </nav>
    <header class="report-cover">
      <div class="report-cover__copy">
        <div v-if="projectContext" class="report-context" aria-label="报告上下文">
          <span>{{ projectContext.title }}</span>
          <strong>正在查看 V{{ review.versionNo || 1 }}</strong>
          <span v-if="projectContext.latestVersionNo > (review.versionNo || 1)">当前最新 V{{ projectContext.latestVersionNo }}</span>
          <RouterLink v-if="projectContext.latestVersionNo > (review.versionNo || 1)" :to="{ name: 'review-detail', params: { id: projectContext.latestReviewId } }">查看最新版本</RouterLink>
        </div>
        <div class="report-stamps">
          <span>产品决策区</span>
          <span>{{ modeLabels[review.mode] }}</span>
          <span v-if="reviewTargetTypeLabel" class="target-type-stamp">{{ reviewTargetTypeLabel }}</span>
          <span class="version-stamp">V{{ review.versionNo || 1 }}</span>
          <span class="decision-stamp" :class="`decision-stamp--${goDecision.toLowerCase()}`">{{ goDecisionLabel }}</span>
          <span v-if="providerBadge" class="provider-stamp">{{ providerBadge }}</span>
        </div>
        <p class="eyebrow">PM 评审纪要 / {{ review.createdAt }}</p>
        <h1>{{ review.oneLineVerdict }}</h1>
        <p class="go-reason">{{ goDecisionReason }}</p>
        <button v-if="review.status === 'SUCCESS'" class="primary-button" type="button" @click="startNextVersion">基于此版本继续优化</button>
        <p v-if="review.status === 'SUCCESS'" class="report-next-hint">进入{{ nextVersionLabel ? '待生成 V' + nextVersionLabel : '下一版本' }} 草稿，提交后才生成新报告；编号以提交结果为准。</p>
        <p v-if="actionFeedback" role="status">{{ actionFeedback }}</p>

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
          <strong>参考验证指标：</strong>{{ successMetric }}（建议值，不是已验证结果）
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
          <DecisionEvidencePanel :review="review" />
          <p>评分是模型对当前描述的辅助判断，不是实测数据或继续投入的依据。</p>
          <ScoreOverviewCard :review="review" />
          <DimensionScoreList :review="review" />
          <KnowledgeBoundaryPanel :review="review" />
          <QualityAuditPanel :review="review" />

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

          <section v-if="report.minimumBuildVersion" id="minimum" class="report-section note-section">
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
              <strong>参考验证指标：</strong>{{ successMetric }}（{{ report.validationGuide?.risks?.[0]?.thresholdReason || '需在测试前结合样本、基线和预算确认；旧报告未提供阈值理由' }}）
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

      <details class="report-more-actions">
        <summary>更多操作</summary>
        <div class="report-toolbar report-toolbar--secondary">
        <button class="secondary-button" type="button" @click="openPackage">
          <PackageCheck :size="17" />
          <span>生成开发任务包</span>
        </button>
        <button class="secondary-button" type="button" @click="shareCardOpen = true">
          <ImageDown :size="17" />
          <span>生成产品体检卡</span>
        </button>
        <button class="secondary-button" type="button" @click="exportReport">
          <Download :size="17" />
          <span>导出 Markdown</span>
        </button>
        <button class="secondary-button" type="button" @click="copyPrompt">
          <ClipboardList :size="17" />
          <span>{{ copied === 'prompt' ? '已复制 Prompt' : '复制开发 Prompt' }}</span>
        </button>
        <button class="secondary-button" type="button" @click="copyReport">
          <Copy :size="17" />
          <span>{{ copied === 'report' ? '已复制报告' : '复制完整报告' }}</span>
        </button>
        </div>
      </details>
      <p v-if="actionFeedback" class="action-feedback" role="status">{{ actionFeedback }}</p>

      <div v-if="packageOpen" class="modal-backdrop" role="presentation" @click.self="packageOpen = false">
        <section class="task-package-modal" role="dialog" aria-modal="true" aria-label="开发任务包">
          <header>
            <div>
              <p class="eyebrow">Development Package</p>
              <h2>开发任务包</h2>
            </div>
            <button class="tiny-copy" type="button" title="关闭" @click="packageOpen = false">
              <X :size="16" />
              <span>关闭</span>
            </button>
          </header>
          <pre>{{ developmentPackage }}</pre>
          <footer>
            <button class="secondary-button" type="button" @click="copyPackage">
              <Copy :size="17" />
              <span>{{ copied === 'package' ? '已复制任务包' : '复制任务包' }}</span>
            </button>
            <button class="secondary-button strong-action" type="button" @click="downloadPackage">
              <Download :size="17" />
              <span>下载 Markdown</span>
            </button>
          </footer>
          <p v-if="actionFeedback" role="status">{{ actionFeedback }}</p>
        </section>
      </div>

      <ShareCardModal v-if="shareCardOpen" :review="review" @close="shareCardOpen = false" />
    </template>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { CircleAlert, ClipboardList, Copy, Download, ImageDown, PackageCheck, X } from '@lucide/vue';

import ListBlock from '@/components/ListBlock.vue';
import DimensionScoreList from '@/components/DimensionScoreList.vue';
import KnowledgeBoundaryPanel from '@/components/KnowledgeBoundaryPanel.vue';
import PixelPig from '@/components/PixelPig.vue';
import QualityAuditPanel from '@/components/QualityAuditPanel.vue';
import ScoreOverviewCard from '@/components/ScoreOverviewCard.vue';
import DecisionEvidencePanel from '@/components/DecisionEvidencePanel.vue';
import ShareCardModal from '@/components/share/ShareCardModal.vue';
import type { ReviewDetailResponse } from '@/types/review';
import { copyText } from '@/utils/clipboard';
import { buildDevelopmentPackage, downloadTextFile } from '@/utils/developmentPackage';
import { modeLabels, reviewTargetTypeLabels } from '@/utils/labels';
import { downloadMarkdown } from '@/utils/markdownExport';
import { getProjectWorkbench } from '@/api/reviewApi';
import { projectDisplayName, reportReturn } from '@/utils/experience';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

const copied = ref<'report' | 'prompt' | 'package' | ''>('');
const packageOpen = ref(false);
const shareCardOpen = ref(false);
const router = useRouter();
const route = useRoute();
const projectContext = ref<{ title: string; latestVersionNo: number; latestReviewId: number } | null>(null);
const actionFeedback = ref('');
const groupId = computed(() => props.review.ideaGroupId || String(props.review.id));
const returnTarget = computed(() => reportReturn(route.query.from, groupId.value));
const report = computed(() => props.review.report);
const developmentPackage = computed(() => buildDevelopmentPackage(props.review));
const boundedBeatScore = computed(() => Math.min(100, Math.max(0, props.review.beatScore || 0)));
const goDecision = computed(() => report.value?.goDecision || 'CONTINUE');
const goDecisionLabel = computed(() => report.value?.validationGuide?.informationInsufficient || !report.value?.validationGuide ? '信息不足，先验证' : decisionLabels[goDecision.value]);
const goDecisionReason = computed(
  () => report.value?.validationGuide?.decisionReason || report.value?.goDecisionReason || '信息不足，先收缩范围并确认真实用户动机。',
);
const reviewTargetTypeLabel = computed(() => {
  const type = report.value?.reviewTargetType;
  return type ? reviewTargetTypeLabels[type] : '';
});
const successMetric = computed(() => report.value?.validationGuide?.risks?.[0]?.referenceThreshold || report.value?.minimumBuildVersion?.successMetric || '');
const validationPlan = computed(() => report.value?.validationGuide?.risks?.flatMap(r => r.steps || []) || report.value?.minimumBuildVersion?.validationPlan || []);
const providerBadge = computed(() => {
  if (!props.review.providerName) {
    return '';
  }
  if (props.review.fallbackUsed) {
    return props.review.providerName === 'mock' ? '模拟兜底结果' : `Fallback: ${props.review.providerName}`;
  }
  return props.review.providerName === 'mock' ? '模拟评审' : props.review.providerName;
});
const nextVersionLabel = computed(() => projectContext.value?.latestVersionNo ? projectContext.value.latestVersionNo + 1 : null);

const decisionLabels = {
  CONTINUE: '建议继续验证',
  PIVOT: '建议调整方向',
  PAUSE: '建议暂缓投入',
};

const sections = [
  { id: 'decision-guide', en: '→', zh: '决策与验证指南' },
  { id: 'pain', en: '01', zh: '用户痛点分析' },
  { id: 'fake-risk', en: '02', zh: '伪需求风险' },
  { id: 'redundancy', en: '03', zh: '功能冗余检查' },
  { id: 'cold-start', en: '04', zh: '冷启动问题' },
  { id: 'mvp', en: '05', zh: 'MVP 改造建议' },
  { id: 'minimum', en: '06', zh: '最小可开发版本' },
  { id: 'validation', en: '07', zh: '验证计划' },
  { id: 'prompt', en: '08', zh: '开发 Prompt' },
];

async function loadProjectContext() {
  projectContext.value = { title: '产品想法项目', latestVersionNo: 0, latestReviewId: props.review.id };
  const id = props.review.id;
  try {
    const project = await getProjectWorkbench(groupId.value);
    const title = await projectDisplayName(project);
    if (id !== props.review.id) return;
    projectContext.value = { title, latestVersionNo: project.latestVersionNo, latestReviewId: project.latestReviewId };
  } catch {
    actionFeedback.value = '最新版本信息暂不可用，可前往项目工作台重试。';
  }
}

async function copyReport() {
  await copyWithFeedback(props.review.reportMarkdown || props.review.oneLineVerdict, 'report');
}

async function copyPrompt() {
  await copyWithFeedback(report.value?.developerPrompt || '', 'prompt');
}

function openPackage() {
  packageOpen.value = true;
}

async function copyPackage() {
  await copyWithFeedback(developmentPackage.value, 'package');
}

function downloadPackage() {
  try {
    const date = props.review.createdAt?.slice(0, 10).replaceAll('-', '') || 'review';
    downloadTextFile(`czspig-dev-package-${date}-${props.review.id}.md`, developmentPackage.value);
    actionFeedback.value = '开发任务包已开始下载。';
  } catch { actionFeedback.value = '下载失败，请重试。'; }
}

async function startNextVersion() {
  await router.push({ name: 'iteration-draft', params: { sourceId: props.review.id }, query: { from: returnTarget.value.path } });
}

async function copyWithFeedback(text: string, type: 'report' | 'prompt' | 'package') {
  try {
    if (!text) throw new Error('empty');
    await copyText(text);
    markCopied(type);
    actionFeedback.value = '已复制。';
  } catch { actionFeedback.value = '复制失败，请重试或手动选择文本复制。'; }
}
function exportReport() {
  try { downloadMarkdown(props.review); actionFeedback.value = '报告 Markdown 已开始下载。'; }
  catch { actionFeedback.value = '导出失败，请重试。'; }
}

function markCopied(type: 'report' | 'prompt' | 'package') {
  copied.value = type;
  window.setTimeout(() => {
    copied.value = '';
  }, 1600);
}

watch(() => props.review.id, loadProjectContext, { immediate: true });
</script>
