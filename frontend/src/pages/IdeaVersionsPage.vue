<template>
  <div class="version-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">Idea Iterations</p>
        <h1>迭代版本</h1>
      </div>
      <RouterLink class="secondary-button" to="/history">
        <ArrowLeft :size="17" />
        <span>返回历史</span>
      </RouterLink>
    </header>

    <div v-if="store.loading" class="loading-state">
      <LoaderCircle class="spin" :size="28" />
      <span>加载迭代版本</span>
    </div>

    <div v-else-if="store.error" class="inline-error">
      <CircleAlert :size="20" />
      <span>{{ store.error }}</span>
    </div>

    <template v-else-if="group">
      <section class="version-timeline">
        <RouterLink
          v-for="version in group.versions"
          :key="version.id"
          class="version-node"
          :to="{ name: 'review-detail', params: { id: version.id } }"
        >
          <span>V{{ version.versionNo }}</span>
          <div>
            <strong>{{ version.oneLineVerdict }}</strong>
            <p>{{ version.createdAt }}</p>
          </div>
        </RouterLink>
      </section>

      <section class="version-table">
        <table>
          <thead>
            <tr>
              <th>版本</th>
              <th>分数变化</th>
              <th>决策变化</th>
              <th>MVP 变化</th>
              <th>验证计划变化</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="version in group.versions" :key="version.id">
              <td>
                <RouterLink :to="{ name: 'review-detail', params: { id: version.id } }">
                  V{{ version.versionNo }}
                </RouterLink>
              </td>
              <td>
                <span>毒打 {{ version.beatScore }}/100</span>
                <small>{{ scoreDelta(version.beatScore, previousOf(version)?.beatScore, 'beat') }}</small>
                <span>定位 {{ version.positioningScore }}/100</span>
                <small>{{ scoreDelta(version.positioningScore, previousOf(version)?.positioningScore, 'positioning') }}</small>
              </td>
              <td>
                <strong>{{ decisionLabels[version.goDecision || 'CONTINUE'] }}</strong>
                <p>{{ version.goDecisionReason }}</p>
              </td>
              <td>
                <p>{{ version.minimumBuildGoal }}</p>
                <strong>核心功能</strong>
                <ul>
                  <li v-for="item in version.coreFeatures" :key="item">{{ item }}</li>
                </ul>
                <strong>暂不做</strong>
                <ul>
                  <li v-for="item in version.excludedFeatures" :key="item">{{ item }}</li>
                </ul>
              </td>
              <td>
                <p v-if="version.successMetric">{{ version.successMetric }}</p>
                <ul>
                  <li v-for="item in version.validationPlan" :key="item">{{ item }}</li>
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
import { computed, onMounted, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { ArrowLeft, CircleAlert, LoaderCircle } from '@lucide/vue';

import { useReviewStore } from '@/stores/reviewStore';
import type { ReviewVersionItem } from '@/types/review';

const route = useRoute();
const store = useReviewStore();
const group = computed(() => store.currentGroup);

const decisionLabels = {
  CONTINUE: '建议继续',
  PIVOT: '建议调整方向',
  PAUSE: '建议暂缓',
};

async function loadGroup() {
  const groupId = String(route.params.groupId || '');
  if (groupId) {
    await store.fetchGroup(groupId);
  }
}

function previousOf(version: ReviewVersionItem) {
  const versions = group.value?.versions ?? [];
  const index = versions.findIndex((item) => item.id === version.id);
  return index > 0 ? versions[index - 1] : null;
}

function scoreDelta(current: number, previous: number | undefined, type: 'beat' | 'positioning') {
  if (previous === undefined) {
    return '首版基线';
  }
  const delta = current - previous;
  if (delta === 0) {
    return '持平';
  }
  const prefix = delta > 0 ? '+' : '';
  const meaning = type === 'beat'
    ? delta < 0 ? '风险下降' : '风险上升'
    : delta > 0 ? '更清晰' : '更模糊';
  return `${prefix}${delta}，${meaning}`;
}

onMounted(loadGroup);
watch(() => route.params.groupId, loadGroup);
</script>
