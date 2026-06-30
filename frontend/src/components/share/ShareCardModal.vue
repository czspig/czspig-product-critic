<template>
  <div class="modal-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="share-card-modal" role="dialog" aria-modal="true" aria-label="产品体检卡">
      <header>
        <div>
          <p class="eyebrow">Health Card</p>
          <h2>产品体检卡</h2>
        </div>
        <button class="tiny-copy" type="button" title="关闭" @click="$emit('close')">
          <X :size="16" />
          <span>关闭</span>
        </button>
      </header>

      <ProductHealthCard :data="cardData" />

      <footer>
        <button class="secondary-button" type="button" @click="copyCardText">
          <Copy :size="17" />
          <span>{{ copied ? '已复制文案' : '复制卡片文案' }}</span>
        </button>
        <button class="secondary-button strong-action" type="button" @click="downloadCard">
          <Download :size="17" />
          <span>下载 PNG</span>
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Copy, Download, X } from '@lucide/vue';

import ProductHealthCard from '@/components/share/ProductHealthCard.vue';
import type { ReviewDetailResponse } from '@/types/review';
import { copyText } from '@/utils/clipboard';
import { buildShareCardText, downloadShareCardPng, toShareCardData } from '@/utils/shareCard';

const props = defineProps<{
  review: ReviewDetailResponse;
}>();

defineEmits<{
  close: [];
}>();

const copied = ref(false);
const cardData = computed(() => toShareCardData(props.review));

async function copyCardText() {
  await copyText(buildShareCardText(props.review));
  copied.value = true;
  window.setTimeout(() => {
    copied.value = false;
  }, 1600);
}

async function downloadCard() {
  await downloadShareCardPng(props.review);
}
</script>
