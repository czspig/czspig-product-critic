<template>
  <div class="history-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">评审记录</p>
        <h1>历史报告</h1>
      </div>
      <RouterLink class="primary-button compact-button" to="/">
        <Plus :size="18" />
        <span>新评审</span>
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
      <RouterLink
        v-for="item in store.history"
        :key="item.id"
        class="history-item"
        :to="{ name: 'review-detail', params: { id: item.id } }"
      >
        <div class="history-main">
          <div class="history-badges">
            <span class="status-pill" :class="item.status.toLowerCase()">{{ statusLabels[item.status] }}</span>
            <span class="status-pill version">V{{ item.versionNo || 1 }}</span>
            <span v-if="item.groupVersionCount > 1" class="status-pill iterated">已迭代 {{ item.groupVersionCount }} 版</span>
          </div>
          <h2>{{ item.inputSummary }}</h2>
          <p>{{ item.status === 'FAILED' ? item.errorMessage : item.oneLineVerdict }}</p>
        </div>
        <div class="history-meta">
          <span>{{ modeLabels[item.mode] }}</span>
          <span>毒打 {{ item.beatScore }}</span>
          <span>{{ item.createdAt }}</span>
        </div>
      </RouterLink>
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
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ChevronLeft, ChevronRight, CircleAlert, Inbox, LoaderCircle, Plus } from '@lucide/vue';

import { useReviewStore } from '@/stores/reviewStore';
import { modeLabels, statusLabels } from '@/utils/labels';

const store = useReviewStore();
const page = ref(1);
const pageSize = 10;
const totalPages = computed(() => Math.max(1, Math.ceil(store.historyTotal / pageSize)));

async function changePage(nextPage: number) {
  page.value = nextPage;
  await store.fetchHistory(page.value, pageSize);
}

onMounted(() => store.fetchHistory(page.value, pageSize));
</script>
