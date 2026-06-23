<template>
  <div class="app-root">
    <TopNav />
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router';

import TopNav from '@/components/TopNav.vue';
</script>
