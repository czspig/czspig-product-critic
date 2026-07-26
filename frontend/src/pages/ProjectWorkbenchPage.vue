<template>
  <div class="project-workbench">
    <div v-if="store.projectLoading" class="loading-state">
      <LoaderCircle class="spin" :size="28" />
      <span>正在汇总项目状态</span>
    </div>

    <div v-else-if="routeError || store.projectError" class="inline-error">
      <CircleAlert :size="20" />
      <span>{{ routeError || store.projectError }}</span>
    </div>

    <template v-else-if="project">
      <header class="project-header">
        <div class="project-heading">
          <p class="eyebrow">Project Workbench</p>
          <h1>{{ project.title || '未命名项目' }}</h1>
          <div class="project-meta" aria-label="项目概况">
            <span>项目 {{ project.projectId }}</span>
            <span>当前 V{{ project.latestVersionNo }}</span>
            <span>共 {{ project.totalVersions }} 个版本</span>
            <span>更新于 {{ project.updatedAt || '暂无时间' }}</span>
          </div>
        </div>
        <div class="project-actions">
          <RouterLink
            class="secondary-button"
            :to="{ name: 'review-detail', params: { id: project.latestReviewId } }"
          >
            <FileText :size="17" />
            <span>最新完整报告</span>
          </RouterLink>
          <button class="secondary-button strong-action" type="button" :disabled="actionLoading" @click="continueProject">
            <RefreshCw :class="{ spin: actionLoading }" :size="17" />
            <span>创建下一版本</span>
          </button>
          <RouterLink
            class="secondary-button"
            :to="{ name: 'idea-versions', params: { groupId: project.projectId } }"
          >
            <GitCompare :size="17" />
            <span>版本对比</span>
          </RouterLink>
        </div>
      </header>

      <div v-if="actionError" class="inline-error">
        <CircleAlert :size="18" />
        <span>{{ actionError }}</span>
      </div>

      <section class="project-decision" aria-labelledby="current-decision-title">
        <div class="project-section-heading">
          <div>
            <p class="eyebrow">Current Decision</p>
            <h2 id="current-decision-title">当前决策</h2>
          </div>
          <span class="decision-chip" :class="`decision-chip--${decision.toLowerCase()}`">
            {{ decisionLabels[decision] }}
          </span>
        </div>
        <p class="project-verdict">{{ project.oneLineVerdict || '当前版本暂无一句话结论' }}</p>
        <p class="project-reason">{{ project.decisionReason || '当前版本暂无决策原因' }}</p>

        <dl class="project-score-strip">
          <div>
            <dt>可信总分</dt>
            <dd>{{ score(project.overallScore) }}</dd>
          </div>
          <div>
            <dt>产品定位</dt>
            <dd>{{ score(project.positioningScore) }}</dd>
          </div>
          <div>
            <dt>风险指数</dt>
            <dd>{{ score(project.beatScore) }}</dd>
          </div>
          <div>
            <dt>质量状态</dt>
            <dd class="quality-value" :class="`quality-value--${qualityStatus.toLowerCase()}`">
              {{ qualityLabels[qualityStatus] }}
            </dd>
          </div>
        </dl>
        <p v-if="project.qualitySummary" class="quality-summary">{{ project.qualitySummary }}</p>
      </section>

      <div class="project-information-grid">
        <section class="project-boundary" aria-labelledby="knowledge-boundary-title">
          <div class="project-section-heading">
            <div>
              <p class="eyebrow">Knowledge Boundary</p>
              <h2 id="knowledge-boundary-title">依据与待确认事项</h2>
            </div>
          </div>

          <div class="project-boundary-grid">
            <div class="boundary-column boundary-column--fact">
              <h3><BadgeCheck :size="17" /> 已知事实</h3>
              <div v-if="project.providedFacts.length" class="boundary-list">
                <article v-for="item in project.providedFacts" :key="`${item.statement}-${item.sourceText}`">
                  <strong>{{ item.statement }}</strong>
                  <p v-if="item.sourceText">原始依据：{{ item.sourceText }}</p>
                </article>
              </div>
              <p v-else class="project-empty-state">当前版本未提取出明确事实</p>
            </div>

            <div class="boundary-column boundary-column--inference">
              <h3><ScanSearch :size="17" /> 当前推断</h3>
              <div v-if="project.inferences.length" class="boundary-list">
                <article v-for="item in project.inferences" :key="`${item.statement}-${item.basis}`">
                  <strong>{{ item.statement }}</strong>
                  <p v-if="item.basis">依据：{{ item.basis }}</p>
                  <span v-if="item.confidence">{{ confidenceLabel(item.confidence) }}</span>
                </article>
              </div>
              <p v-else class="project-empty-state">当前版本暂无推断</p>
            </div>

            <div class="boundary-column boundary-column--hypothesis">
              <h3><FlaskConical :size="17" /> 关键假设</h3>
              <div v-if="project.criticalHypotheses.length" class="boundary-list">
                <article v-for="item in project.criticalHypotheses" :key="item.statement">
                  <strong>{{ item.statement }}</strong>
                  <p v-if="item.impactIfFalse">若不成立：{{ item.impactIfFalse }}</p>
                  <span v-if="item.importance">优先级：{{ confidenceLabel(item.importance) }}</span>
                </article>
              </div>
              <p v-else class="project-empty-state">当前版本暂无关键假设</p>
            </div>

            <div class="boundary-column boundary-column--unknown">
              <h3><CircleHelp :size="17" /> 仍需确认</h3>
              <div v-if="project.unknownQuestions.length" class="boundary-list">
                <article v-for="item in project.unknownQuestions" :key="item.question">
                  <strong>{{ item.question }}</strong>
                  <p v-if="item.reason">原因：{{ item.reason }}</p>
                </article>
              </div>
              <p v-else class="project-empty-state">当前版本暂无未知问题</p>
            </div>
          </div>
        </section>

        <section class="project-validation" aria-labelledby="validation-plan-title">
          <div class="project-section-heading">
            <div>
              <p class="eyebrow">Validation Plan</p>
              <h2 id="validation-plan-title">下一步验证</h2>
            </div>
            <ListChecks :size="22" />
          </div>
          <div class="success-target">
            <span>成功指标</span>
            <strong>{{ project.successMetric || '当前版本尚未定义成功指标' }}</strong>
          </div>
          <ol v-if="project.validationPlan.length" class="validation-steps">
            <li v-for="(item, index) in project.validationPlan" :key="`${index}-${item}`">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <p>{{ item }}</p>
            </li>
          </ol>
          <p v-else class="project-empty-state">当前版本暂无验证动作</p>
        </section>
      </div>

      <section class="project-timeline" aria-labelledby="version-timeline-title">
        <div class="project-section-heading">
          <div>
            <p class="eyebrow">Version Timeline</p>
            <h2 id="version-timeline-title">版本时间线</h2>
          </div>
          <RouterLink
            class="secondary-button"
            :to="{ name: 'idea-versions', params: { groupId: project.projectId } }"
          >
            <GitCompare :size="17" />
            <span>完整对比</span>
          </RouterLink>
        </div>

        <div v-if="project.versions.length" class="project-version-list">
          <RouterLink
            v-for="version in project.versions"
            :key="version.id"
            class="project-version-item"
            :class="{ 'project-version-item--current': version.id === project.latestReviewId }"
            :to="{ name: 'review-detail', params: { id: version.id } }"
          >
            <div class="version-item-heading">
              <span>V{{ version.versionNo }}</span>
              <small>{{ version.createdAt || '暂无时间' }}</small>
            </div>
            <strong>{{ version.oneLineVerdict || '暂无一句话结论' }}</strong>
            <p>{{ decisionLabels[version.goDecision || 'CONTINUE'] }}</p>
            <div class="version-item-footer">
              <span>定位 {{ version.positioningScore ?? 0 }}</span>
              <span>{{ qualityLabel(version.qualityStatus) }}</span>
              <ArrowUpRight :size="16" />
            </div>
          </RouterLink>
        </div>
        <p v-else class="project-empty-state">暂无版本记录</p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import {
  ArrowUpRight,
  BadgeCheck,
  CircleAlert,
  CircleHelp,
  FileText,
  FlaskConical,
  GitCompare,
  ListChecks,
  LoaderCircle,
  RefreshCw,
  ScanSearch,
} from '@lucide/vue';

import { useReviewStore } from '@/stores/reviewStore';
import type { ConfidenceLevel, GoDecision, QualityStatus } from '@/types/review';
import { buildNextDraft } from '@/utils/developmentPackage';

const route = useRoute();
const router = useRouter();
const store = useReviewStore();
const routeError = ref('');
const actionError = ref('');
const actionLoading = ref(false);
const project = computed(() => store.currentProject);
const decision = computed<GoDecision>(() => project.value?.decision || 'CONTINUE');
const qualityStatus = computed<QualityStatus>(() => project.value?.qualityStatus || 'WARNING');

const decisionLabels: Record<GoDecision, string> = {
  CONTINUE: '继续推进',
  PIVOT: '调整方向',
  PAUSE: '暂缓投入',
};

const qualityLabels: Record<QualityStatus, string> = {
  PASS: '通过',
  WARNING: '需注意',
  FAIL: '未通过',
};

async function loadProject() {
  const groupId = String(route.params.groupId || '').trim();
  if (!groupId || groupId.length > 64 || !/^[A-Za-z0-9_-]+$/.test(groupId)) {
    routeError.value = '项目标识不合法';
    store.clearProject();
    return;
  }
  routeError.value = '';
  actionError.value = '';
  try {
    await store.fetchProject(groupId);
  } catch {
    // The store exposes the API message through projectError.
  }
}

async function continueProject() {
  if (!project.value || actionLoading.value) return;
  actionLoading.value = true;
  actionError.value = '';
  try {
    const review = await store.fetchReview(project.value.latestReviewId);
    store.prepareDraftFromReview(review, buildNextDraft(review));
    await router.push({ name: 'home' });
  } catch {
    actionError.value = store.error || '无法创建下一版本，请稍后重试';
  } finally {
    actionLoading.value = false;
  }
}

function score(value?: number) {
  return value === undefined || value === null ? '--' : `${value}/100`;
}

function confidenceLabel(value: ConfidenceLevel) {
  return ({ HIGH: '高', MEDIUM: '中', LOW: '低' } as const)[value] || value;
}

function qualityLabel(value?: QualityStatus) {
  return qualityLabels[value || 'WARNING'];
}

onMounted(loadProject);
watch(() => route.params.groupId, loadProject);
</script>
