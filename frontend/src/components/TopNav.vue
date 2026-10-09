<template>
  <header class="top-nav">
    <RouterLink class="brand" to="/" aria-label="返回首页">
      <span class="brand-mark"><PixelPig size="small" pose="badge" /></span>
      <span class="brand-text">猪猪产品毒舌官</span>
    </RouterLink>
    <nav class="top-auth" aria-label="账户入口">
      <template v-if="account.user">
        <RouterLink class="top-account" to="/account/profile"><span class="initial-avatar">{{ account.user.nickname.slice(0, 1) }}</span>{{ account.user.nickname }}</RouterLink>
      </template>
      <template v-else>
        <span class="top-anonymous">匿名体验 · 剩余 {{ account.remaining }} 次</span>
        <RouterLink class="top-auth-link" :to="loginTarget">登录</RouterLink>
        <RouterLink class="top-auth-link top-auth-link--primary" :to="registerTarget">注册</RouterLink>
      </template>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';

import PixelPig from '@/components/PixelPig.vue';
import { computed } from 'vue';
import { account } from '@/stores/account';
import { useRoute } from 'vue-router';

const route = useRoute();
const loginTarget = computed(() => ({ path: '/account/login', query: route.path.startsWith('/account/') ? {} : { returnTo: route.fullPath } }));
const registerTarget = computed(() => ({ ...loginTarget.value, path: '/account/register' }));
</script>
