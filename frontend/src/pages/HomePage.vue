<template>
  <div class="workspace-grid">
    <ReviewInputForm :loading="store.loading" @submit="handleSubmit" />

    <aside class="result-panel">
      <div v-if="store.error" class="inline-error">
        <CircleAlert :size="20" />
        <span>{{ store.error }}</span>
      </div>

      <div v-if="store.currentReview" class="latest-result">
        <p class="eyebrow">最近一次</p>
        <h2>{{ store.currentReview.oneLineVerdict }}</h2>
        <div class="mini-scores">
          <span>毒打 {{ store.currentReview.beatScore }}</span>
          <span>定位 {{ store.currentReview.positioningScore }}</span>
        </div>
        <RouterLink class="secondary-button wide" :to="{ name: 'review-detail', params: { id: store.currentReview.id } }">
          <FileText :size="17" />
          <span>查看报告</span>
        </RouterLink>
      </div>

      <div v-else class="empty-result">
        <Gauge :size="42" />
        <h2>等待评审输入</h2>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { useRouter, RouterLink } from 'vue-router';
import { CircleAlert, FileText, Gauge } from '@lucide/vue';

import ReviewInputForm from '@/components/ReviewInputForm.vue';
import { useReviewStore } from '@/stores/reviewStore';
import type { CreateReviewPayload } from '@/types/review';

const router = useRouter();
const store = useReviewStore();

async function handleSubmit(payload: CreateReviewPayload) {
  const review = await store.create(payload);
  await router.push({ name: 'review-detail', params: { id: review.id } });
}
</script>
