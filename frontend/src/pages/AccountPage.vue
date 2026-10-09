<template>
  <section class="account-panel panel">
    <RouterLink to="/projects">返回项目入口</RouterLink>
    <h1>{{ titles[mode] || '个人中心' }}</h1>
    <p v-if="route.query.changed" role="status">密码已修改，所有旧会话已失效，请重新登录。</p>
    <p v-if="route.query.expired" role="status">登录已过期或密码已修改，请重新登录。验证身份后可返回此前页面。</p>
    <template v-if="mode === 'profile' && account.user">
      <p>用户名：{{ account.user.username }}（不可修改）</p>
      <p>绑定邮箱：{{ account.user.email }} · 已验证</p>
      <form @submit.prevent="saveNickname"><label>昵称<input v-model="nickname" required maxlength="64" /></label><button :disabled="busy">保存昵称</button></form>
      <h2>修改密码</h2>
      <form @submit.prevent="changePassword">
        <label>当前密码<input v-model="currentPassword" type="password" autocomplete="current-password" required /></label>
        <label>新密码<input v-model="password" type="password" autocomplete="new-password" required minlength="10" maxlength="72" /></label>
        <p>修改后所有设备需重新登录。</p><button :disabled="busy">修改密码并重新登录</button>
      </form>
      <button :disabled="busy" @click="exit">退出登录</button>
    </template>
    <div v-else-if="mode === 'login' && account.user"><p>已登录为 {{ account.user.nickname }}</p><button v-if="!migrationPrompt" @click="finish">进入我的项目</button></div>
    <form v-else @submit.prevent="submit">
      <template v-if="mode === 'login' || mode === 'register'">
        <label>用户名<input v-model="username" required minlength="3" maxlength="32" autocomplete="username" pattern="[A-Za-z0-9_]{3,32}" /></label>
        <p v-if="mode === 'register'">3–32 位字母、数字或下划线；不区分大小写。</p>
      </template>
      <label v-if="mode === 'register'">昵称<input v-model="nickname" required maxlength="64" autocomplete="nickname" /></label>
      <label v-if="['register', 'forgot', 'resend'].includes(mode)">邮箱<input v-model="email" type="email" required maxlength="254" autocomplete="email" /></label>
      <label v-if="['login', 'register', 'reset'].includes(mode)">{{ mode === 'reset' ? '新密码' : '密码' }}<input v-model="password" type="password" required :minlength="mode === 'login' ? 1 : 10" maxlength="72" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" /></label>
      <p v-if="['register', 'reset'].includes(mode)">至少 10 位；建议使用密码管理器生成独立密码。</p>
      <p v-if="mode === 'verify'">确认验证注册邮箱，验证后即可登录。</p>
      <button class="primary-button" :disabled="busy || (mode === 'profile' && !account.user)">{{ busy ? '处理中…' : actions[mode] || '请先登录' }}</button>
    </form>
    <p v-if="message" role="status">{{ message }}</p><p v-if="error" role="alert" class="error-text">{{ error }}</p>
    <nav v-if="!account.user" class="account-links"><RouterLink to="/account/login">登录</RouterLink><RouterLink to="/account/register">注册</RouterLink><RouterLink to="/account/forgot">忘记密码</RouterLink><RouterLink to="/account/resend">重发验证邮件</RouterLink></nav>
    <RouterLink to="/">{{ account.user ? '返回评审入口' : '继续匿名体验' }}</RouterLink>
    <div v-if="migrationPrompt" class="panel">
      <h2>是否将本浏览器的匿名项目加入账号？</h2><p>加入整个项目及其所有版本和对应草稿。也可暂时保持独立。</p>
      <button :disabled="busy" @click="migrate">加入账号</button><button :disabled="busy" @click="finish">暂不加入</button>
    </div>
  </section>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { http, toErrorMessage } from '@/api/http';
import { account, login, logout, loadSession, migrateProjects, safeReturn, clearAccountDrafts, broadcastAccountChange } from '@/stores/account';
const route = useRoute(), router = useRouter();
const mode = computed(() => String(route.params.mode || 'login'));
const titles: Record<string, string> = { login: '登录', register: '注册账号', verify: '验证邮箱', forgot: '找回密码', reset: '重置密码', resend: '重发验证邮件', profile: '个人中心' };
const actions: Record<string, string> = { login: '登录', register: '注册并验证邮箱', verify: '验证邮箱', forgot: '申请重置邮件', reset: '重置密码', resend: '重发验证邮件' };
const username = ref(''), nickname = ref(account.user?.nickname || ''), email = ref(''), password = ref(''), currentPassword = ref('');
const busy = ref(false), error = ref(''), message = ref(''), migrationPrompt = ref(mode.value === 'login' && !!account.user && account.anonymousProjects > 0);
watch(mode, () => { error.value = ''; message.value = ''; password.value = ''; currentPassword.value = ''; migrationPrompt.value = false; });
async function run(fn: () => Promise<void>) { if (busy.value) return; busy.value = true; error.value = ''; message.value = ''; try { await fn(); } catch (e) { error.value = toErrorMessage(e); } finally { busy.value = false; } }
try { if (route.query.returnTo) sessionStorage.setItem('czspig-account-return', safeReturn(route.query.returnTo)); } catch { }
async function finish() {
  migrationPrompt.value = false; let remembered: string | null = null;
  try { remembered = sessionStorage.getItem('czspig-account-return'); sessionStorage.removeItem('czspig-account-return'); } catch { }
  let target = safeReturn(route.query.returnTo || remembered);
  const match = target.match(/^\/(projects|ideas|reviews|drafts)\/([^?]+)/);
  const result = target.match(/^\/\?result=(\d+)$/);
  try {
    if (match) await http.get(['projects', 'ideas'].includes(match[1]) ? `/projects/${match[2]}` : `/reviews/${match[2]}`);
    if (result) await http.get(`/reviews/${result[1]}`);
  } catch { target = '/projects'; }
  await router.replace(target);
}
async function submit() { await run(async () => {
  if (mode.value === 'login') { await login(username.value, password.value); password.value = ''; if (account.anonymousProjects) migrationPrompt.value = true; else await finish(); return; }
  const payload = mode.value === 'register' ? { username: username.value, nickname: nickname.value, email: email.value, password: password.value } : ['verify', 'reset'].includes(mode.value) ? { token: route.query.token || '', ...(mode.value === 'reset' ? { password: password.value } : {}) } : { email: email.value };
  const response = await http.post(`/account/${mode.value}`, payload); message.value = response.data.data.message; password.value = '';
}); }
async function migrate() { await run(async () => { await migrateProjects(); await finish(); }); }
async function saveNickname() { await run(async () => { await http.patch('/account/profile', { nickname: nickname.value }); await loadSession(); message.value = '昵称已保存'; }); }
async function changePassword() { await run(async () => { await http.post('/account/password', { currentPassword: currentPassword.value, password: password.value }); clearAccountDrafts(); broadcastAccountChange(); await loadSession(); password.value = ''; currentPassword.value = ''; window.location.replace('/account/login?changed=1'); }); }
async function exit() { await run(async () => { await logout(); window.location.replace('/'); }); }
</script>
