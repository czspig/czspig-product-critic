<template>
  <article class="health-card-preview">
    <header>
      <div>
        <span class="health-card-preview__stamp">产品想法体检卡</span>
        <span v-if="data.versionLabel" class="health-card-preview__version">{{ data.versionLabel }}</span>
      </div>
      <PixelPig size="medium" />
    </header>

    <p class="eyebrow">猪猪产品毒舌官</p>
    <h2>{{ data.title }}</h2>

    <section class="health-card-preview__decision">
      <span>决策结论</span>
      <strong>{{ data.decision }}</strong>
    </section>

    <div class="health-card-preview__scores">
      <section>
        <span>毒打指数</span>
        <strong>{{ data.beatScore }}/100</strong>
      </section>
      <section>
        <span>产品定位评分</span>
        <strong>{{ data.positioningScore }}/100</strong>
      </section>
    </div>

    <section class="health-card-preview__note">
      <span>一句话评价</span>
      <p>{{ data.oneLineVerdict }}</p>
    </section>

    <section class="health-card-preview__note">
      <span>成功指标</span>
      <p>{{ data.successMetric }}</p>
    </section>

    <footer>由「猪猪产品毒舌官」生成</footer>
  </article>
</template>

<script setup lang="ts">
import PixelPig from '@/components/PixelPig.vue';
import type { ShareCardData } from '@/utils/shareCard';

defineProps<{
  data: ShareCardData;
}>();
</script>
