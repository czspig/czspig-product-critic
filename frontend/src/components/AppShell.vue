<template>
  <div class="app-root">
    <ProjectSidebar />
    <TopNav />
    <main class="app-main">
      <RouterView :key="$route.path.startsWith('/account/') ? $route.fullPath : account.identityKey + $route.fullPath.split('#')[0] + revision" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router';

import TopNav from '@/components/TopNav.vue';
import ProjectSidebar from '@/components/ProjectSidebar.vue';
import { account } from '@/stores/account';
import { onMounted, onUnmounted, ref } from 'vue';
const revision = ref(0);
function refreshPage() { revision.value++; }
onMounted(() => window.addEventListener('project-renamed', refreshPage));
onUnmounted(() => window.removeEventListener('project-renamed', refreshPage));
</script>
