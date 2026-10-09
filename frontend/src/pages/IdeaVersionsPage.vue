<template>
  <div class="version-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">Idea Iterations</p>
        <h1>{{ displayName }} · 版本历史</h1>
        <p v-if="group">当前最新 V{{ latestVersion }} · 点击版本查看对应报告</p>
      </div>
      <div class="page-toolbar">
        <RouterLink class="secondary-button" :to="returnTarget.path">
          <ArrowLeft :size="17" />
          <span>{{ returnTarget.label }}</span>
        </RouterLink>
      </div>
    </header>

    <div v-if="store.loading" class="loading-state">
      <LoaderCircle class="spin" :size="28" />
      <span>加载迭代版本</span>
    </div>

    <div v-else-if="store.error" class="inline-error">
      <CircleAlert :size="20" />
      <span>{{ store.error }}</span>
      <button class="secondary-button" @click="loadGroup">重新加载版本</button>
    </div>

    <template v-else-if="group">
      <section class="version-timeline">
        <RouterLink
          v-for="version in group.versions"
          :key="version.id"
          class="version-node"
          :to="{ name: 'review-detail', params: { id: version.id }, query: { from: route.path } }"
        >
          <span>V{{ version.versionNo }}</span>
          <div>
            <strong>{{ version.oneLineVerdict || '暂无一句话评价' }}</strong>
            <p>{{ version.createdAt || '暂无时间' }}</p>
          </div>
        </RouterLink>
      </section>

      <section v-if="comparisonSummary" class="version-change-guide" aria-label="版本变化说明">
        <p class="eyebrow">Change Evidence</p>
        <h2>这次修改解决了什么？</h2>
        <label>查看变化的版本<select v-model="selectedVersionId"><option v-for="v in group.versions" :key="v.id" :value="v.id">V{{ v.versionNo }}</option></select></label>
        <p v-if="!comparisonSummary.parent">首版或来源版本缺失，无法建立可靠对照。</p>
        <template v-else>
          <p>与实际来源 V{{ comparisonSummary.parent.versionNo }} 对比；输入变化按原文语句比较，不推断语义。</p>
          <p v-if="comparisonSummary.unavailable">旧数据缺少完整输入，无法可靠重建修改。</p>
          <details><summary>新增或改写的输入（{{ comparisonSummary.added.length }}）</summary><ul><li v-for="s in comparisonSummary.added" :key="s">{{ s }}</li></ul></details>
          <details><summary>删除或被替换的输入（{{ comparisonSummary.removed.length }}）</summary><ul><li v-for="s in comparisonSummary.removed" :key="s">{{ s }}</li></ul></details>
          <p><strong>仍被提出的风险：</strong>{{ comparisonSummary.persistentRisks.join('；') || '未找到相同风险表述；不代表已解决。' }}</p>
          <p><strong>未再提出的风险：</strong>{{ comparisonSummary.omittedRisks.join('；') || '无' }}。措辞变化或省略不能证明已缓解。</p>
          <p><strong>新增风险提示：</strong>{{ comparisonSummary.newRisks.join('；') || '无新增相同粒度提示' }}</p>
          <p><strong>已由证据证明缓解：</strong>系统没有核验外部验证记录，无法确认；方案描述更完善与风险实证降低须分别判断。</p>
          <p><strong>下一版最值得补充：</strong>{{ selectedVersion?.validationGuide?.risks?.[0]?.evidenceToCollect?.join('；') || selectedVersion?.validationPlan?.[0] || '补充目标用户的任务过程、行为记录和反例。' }}</p>
        </template>
      </section>

      <section class="version-table">
        <table>
          <thead>
            <tr>
              <th>版本</th>
              <th>分数变化</th>
              <th>可信度</th>
              <th>决策变化</th>
              <th>MVP 变化</th>
              <th>验证计划变化</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="version in group.versions" :key="version.id">
              <td>
                <RouterLink :to="{ name: 'review-detail', params: { id: version.id }, query: { from: route.path } }">
                  V{{ version.versionNo }}
                </RouterLink>
              </td>
              <td>
                <span>毒打 {{ version.beatScore ?? 0 }}/100</span>
                <small>{{ scoreDelta(version.beatScore, previousOf(version)?.beatScore, 'beat') }}</small>
                <span>定位 {{ version.positioningScore ?? 0 }}/100</span>
                <small>{{ scoreDelta(version.positioningScore, previousOf(version)?.positioningScore, 'positioning') }}</small>
                <small v-if="version.scoreChangeReason">{{ version.scoreChangeReason }}</small>
              </td>
              <td>
                <strong>{{ qualityStatusLabel(version.qualityStatus) }}</strong>
                <p>{{ version.confidenceSummary || '暂无置信度摘要' }}</p>
                <ul>
                  <li v-for="item in nonEmpty(version.criticalHypotheses)" :key="item">{{ item }}</li>
                </ul>
              </td>
              <td>
                <strong>{{ !version.validationGuide || version.validationGuide.informationInsufficient ? '信息不足，先验证' : decisionLabels[version.goDecision || 'CONTINUE'] }}</strong>
                <p>{{ version.validationGuide?.decisionReason || version.goDecisionReason || '暂无决策原因' }}</p>
              </td>
              <td>
                <p>{{ version.minimumBuildGoal || '暂无最小版本目标' }}</p>
                <strong>核心功能</strong>
                <ul>
                  <li v-for="item in nonEmpty(version.coreFeatures)" :key="item">{{ item }}</li>
                </ul>
                <strong>暂不做</strong>
                <ul>
                  <li v-for="item in nonEmpty(version.excludedFeatures)" :key="item">{{ item }}</li>
                </ul>
              </td>
              <td>
                <p>参考判据：{{ version.validationGuide?.risks?.[0]?.referenceThreshold || version.successMetric || '尚缺判据' }}</p>
                <small>{{ version.validationGuide?.risks?.[0]?.thresholdReason || '旧报告缺少阈值依据，测试前需结合样本与预算确认。' }}</small>
                <ul>
                  <li v-for="item in nonEmpty(version.validationGuide?.risks?.flatMap(r => r.steps || []) || version.validationPlan)" :key="item">{{ item }}</li>
                </ul>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { ArrowLeft, CircleAlert, LoaderCircle } from '@lucide/vue';

import { useReviewStore } from '@/stores/reviewStore';
import type { QualityStatus, ReviewVersionItem } from '@/types/review';
import { getProjectWorkbench } from '@/api/reviewApi';
import { projectDisplayName } from '@/utils/experience';
import { compareVersionEvidence } from '@/utils/versionEvidence';

const route = useRoute();
const store = useReviewStore();
const group = computed(() => store.currentGroup);
const displayName = ref('产品想法项目');
const latestVersion = computed(() => Math.max(...(group.value?.versions.map(v => v.versionNo) || [1])));
const returnTarget = computed(() => {
  const from = route.query.from;
  const validReport = typeof from === 'string' && group.value?.versions.some(v => from === '/reviews/' + v.id);
  return validReport ? { path: from as string, label: '返回 V' + group.value?.versions.find(v => from === '/reviews/' + v.id)?.versionNo + ' 报告' }
    : { path: '/projects/' + route.params.groupId, label: '返回项目工作台' };
});
const selectedVersionId = ref<number | null>(null);
const selectedVersion = computed(() => group.value?.versions.find(v => v.id === selectedVersionId.value) || group.value?.versions.at(-1));
const comparisonSummary = computed(() => selectedVersion.value ? compareVersionEvidence(selectedVersion.value, group.value?.versions || []) : null);

const decisionLabels = {
  CONTINUE: '建议继续',
  PIVOT: '建议调整方向',
  PAUSE: '建议暂缓',
};

async function loadGroup() {
  const groupId = String(route.params.groupId || '');
  if (groupId) {
    try {
      await store.fetchGroup(groupId);
      selectedVersionId.value = group.value?.versions.at(-1)?.id || null;
      displayName.value = await projectDisplayName(await getProjectWorkbench(groupId));
    } catch { /* Store exposes loading errors. */ }
  }
}

function previousOf(version: ReviewVersionItem) {
  return group.value?.versions.find(item => item.id === version.parentReviewId) || null;
}

function scoreDelta(current: number | undefined, previous: number | undefined, type: 'beat' | 'positioning') {
  if (current === undefined || current === null) {
    return '暂无分数';
  }
  if (previous === undefined || previous === null) {
    return '首版基线';
  }
  const delta = current - previous;
  if (delta === 0) {
    return '持平';
  }
  const prefix = delta > 0 ? '+' : '';
  const meaning = type === 'beat'
    ? '模型风险评分变化，尚未核验'
    : '模型描述评分变化，尚未核验';
  return `${prefix}${delta}，${meaning}`;
}

function qualityStatusLabel(value?: QualityStatus) {
  if (value === 'PASS') return '质量通过';
  if (value === 'FAIL') return '质量未通过';
  if (value === 'WARNING') return '质量需注意';
  return '暂无自检';
}

function nonEmpty(items: string[] | undefined) {
  return items && items.length > 0 ? items : ['暂无'];
}

onMounted(loadGroup);
watch(() => route.params.groupId, loadGroup);
</script>
