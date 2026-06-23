<template>
  <header class="top-nav">
    <RouterLink class="brand" to="/" aria-label="返回首页">
      <span class="brand-mark">猪</span>
      <span class="brand-text">猪猪产品毒舌官</span>
    </RouterLink>

    <nav class="nav-links" aria-label="主导航">
      <RouterLink to="/" class="nav-link">
        <FilePenLine :size="18" />
        <span>评审</span>
      </RouterLink>
      <RouterLink to="/history" class="nav-link">
        <History :size="18" />
        <span>历史</span>
      </RouterLink>
    </nav>

    <button class="ghost-icon" type="button" title="登录能力已预留，MVP 暂未开放" disabled>
      <UserRound :size="18" />
      <span>登录</span>
    </button>
  </header>
</template>

<script setup lang="ts">
import { FilePenLine, History, UserRound } from '@lucide/vue';
import { RouterLink } from 'vue-router';
</script>
