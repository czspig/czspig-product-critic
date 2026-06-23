<template>
  <div class="not-found">
    <h1>页面不存在</h1>
    <RouterLink class="primary-button compact-button" to="/">
      <ArrowLeft :size="18" />
      <span>返回评审</span>
    </RouterLink>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@lucide/vue';
import { RouterLink } from 'vue-router';
</script>
