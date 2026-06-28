<template>
  <div class="page-stack">
    <div v-if="store.loading" class="loading-state">
      <LoaderCircle class="spin" :size="28" />
      <span>正在整理评审会议纪要</span>
    </div>

    <div v-else-if="store.error" class="inline-error">
      <CircleAlert :size="20" />
      <span>{{ store.error }}</span>
    </div>

    <ReportView v-else-if="store.currentReview" :review="store.currentReview" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { CircleAlert, LoaderCircle } from '@lucide/vue';

import ReportView from '@/components/ReportView.vue';
import { useReviewStore } from '@/stores/reviewStore';

const route = useRoute();
const store = useReviewStore();

async function loadReview() {
  const id = Number(route.params.id);
  if (Number.isFinite(id) && id > 0) {
    await store.fetchReview(id);
  }
}

onMounted(loadReview);
watch(() => route.params.id, loadReview);
</script>
