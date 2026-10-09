<template>
  <div class="history-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">评审记录</p>
        <h1>历史记录</h1>
      </div>
      <RouterLink class="primary-button compact-button" to="/">
        <Plus :size="18" />
        <span>新建评审</span>
      </RouterLink>
    </header>

    <div v-if="store.historyLoading" class="loading-state">
      <LoaderCircle class="spin" :size="28" />
      <span>加载历史</span>
    </div>

    <div v-else-if="store.error" class="inline-error">
      <CircleAlert :size="20" />
      <span>{{ store.error }}</span>
    </div>

    <div v-else-if="store.history.length === 0" class="empty-history">
      <Inbox :size="40" />
      <h2>暂无记录</h2>
    </div>

    <section v-else class="history-list">
      <article
        v-for="item in store.history"
        :key="item.id"
        class="history-item"
      >
        <div class="history-main">
          <div class="history-badges">
            <span class="status-pill" :class="item.status.toLowerCase()">{{ statusLabels[item.status] }}</span>
            <span class="status-pill version">V{{ item.versionNo || 1 }}</span>
            <span class="status-pill iterated">项目最新 {{ projects[item.ideaGroupId] ? 'V' + projects[item.ideaGroupId].latestVersionNo : metadataLoading ? '加载中' : '暂不可用' }}</span>
          </div>
          <h2>{{ projects[item.ideaGroupId]?.displayName || '产品想法项目' }}</h2>
          <p>{{ item.inputSummary }}</p>
          <p>{{ item.status === 'FAILED' ? item.errorMessage : item.oneLineVerdict }}</p>
        </div>
        <div class="history-meta">
          <span>{{ modeLabels[item.mode] }}</span>
          <span>毒打 {{ item.beatScore }}</span>
          <span>{{ item.createdAt }}</span>
        </div>
        <div class="page-toolbar">
          <RouterLink class="secondary-button" :to="{ name: 'review-detail', params: { id: item.id }, query: { from: route.fullPath } }">查看报告</RouterLink>
          <RouterLink class="secondary-button" :to="{ name: 'project-workbench', params: { groupId: item.ideaGroupId || item.id } }">查看项目</RouterLink>
        </div>
      </article>
    </section>

    <footer v-if="store.historyTotal > 10" class="pagination">
      <button class="secondary-button" type="button" :disabled="page <= 1" @click="changePage(page - 1)">
        <ChevronLeft :size="17" />
        <span>上一页</span>
      </button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button class="secondary-button" type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">
        <span>下一页</span>
        <ChevronRight :size="17" />
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { ChevronLeft, ChevronRight, CircleAlert, Inbox, LoaderCircle, Plus } from '@lucide/vue';

import { useReviewStore } from '@/stores/reviewStore';
import { modeLabels, statusLabels } from '@/utils/labels';
import { getProjectWorkbench } from '@/api/reviewApi';
import { projectDisplayName } from '@/utils/experience';

const store = useReviewStore();
const route = useRoute();
const router = useRouter();
const page = computed(() => Math.max(1, Number(route.query.page) || 1));
const projects = ref<Record<string, { latestVersionNo: number; displayName: string }>>({});
const metadataLoading = ref(false);
const pageSize = 10;
const totalPages = computed(() => Math.max(1, Math.ceil(store.historyTotal / pageSize)));

async function changePage(nextPage: number) {
  await router.push({ name: 'history', query: { page: nextPage } });
}

async function loadHistory() {
  metadataLoading.value = true;
  try {
    await store.fetchHistory(page.value, pageSize);
    await Promise.all([...new Set(store.history.map(i => i.ideaGroupId || String(i.id)))].map(async id => {
      try {
        const project = await getProjectWorkbench(id);
        projects.value[id] = { latestVersionNo: project.latestVersionNo, displayName: await projectDisplayName(project) };
      } catch { /* Report links remain usable when project metadata cannot load. */ }
    }));
  } catch { /* Store exposes the request error. */ }
  finally { metadataLoading.value = false; }
}
watch(() => route.query.page, loadHistory, { immediate: true });
</script>
