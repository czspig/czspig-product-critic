<template><section class="panel account-panel"><h1>我的项目</h1><p>从侧边栏搜索或打开项目工作台，继续查看报告和编辑版本。</p><RouterLink class="primary-button" to="/">新建评审</RouterLink><p v-if="!account.user">匿名项目保存在本浏览器。登录后可选择加入账号。</p><p v-if="account.user && account.anonymousProjects">本浏览器还有 {{ account.anonymousProjects }} 个独立的匿名项目。</p><button v-if="account.user && account.anonymousProjects" :disabled="busy" @click="migrate">将匿名项目加入账号</button><p v-if="error" role="alert">{{ error }}</p></section></template>
<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import { account, migrateProjects } from '@/stores/account';
import { toErrorMessage } from '@/api/http';
const busy = ref(false), error = ref('');
async function migrate() { if (!window.confirm('将本浏览器的匿名项目、所有版本和对应草稿加入当前账号？')) return; busy.value = true; try { await migrateProjects(); } catch (e) { error.value = toErrorMessage(e); } finally { busy.value = false; } }
</script>
