<template>
  <button class="sidebar-toggle" @click="opened = !opened" :aria-expanded="opened" aria-controls="project-sidebar">{{ opened ? '收起项目' : '打开项目' }}</button>
  <button v-if="opened" class="sidebar-backdrop" aria-label="关闭项目菜单" @click="opened = false"></button>
  <aside id="project-sidebar" class="project-sidebar" :class="{ collapsed: !opened }" aria-label="项目导航">
    <RouterLink class="primary-button" to="/" @click.prevent="openNewReview">＋ 新建评审</RouterLink>
    <form @submit.prevent="searchProjects"><label>搜索项目<input v-model="search" type="search" placeholder="搜索我的全部项目" /></label><button :disabled="loading">搜索</button></form>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">正在加载项目…</p>
    <div class="sidebar-projects">
      <div v-for="p in items" :key="p.projectId" class="sidebar-project" :class="{ selected: currentProject === p.projectId }">
        <RouterLink :to="`/projects/${p.projectId}`" :aria-current="currentProject === p.projectId ? 'page' : undefined" :title="p.name" @click="closeMobile">{{ p.name }}<small>最新 V{{ p.latestVersion }}</small></RouterLink>
        <button :aria-label="`管理项目 ${p.name}`" @click="editing = p; rename = p.name; deleting = false">•••</button>
      </div>
      <p v-if="!loading && !items.length">{{ appliedSearch ? '未找到项目' : '还没有项目，从新建评审开始' }}</p>
      <button v-if="items.length < total" :disabled="loading" @click="fetchProjects(true)">加载更多项目</button>
    </div>
    <footer>
      <RouterLink class="sidebar-nav-link" to="/history"><span class="sidebar-nav-icon">↗</span>历史报告</RouterLink>
      <template v-if="account.user"><RouterLink to="/account/profile" class="sidebar-profile"><span class="initial-avatar">{{ account.user.nickname.slice(0, 1) }}</span><span><strong>{{ account.user.nickname }}</strong><small>个人中心</small></span></RouterLink><button class="sidebar-exit" :disabled="leaving" @click="exit">退出登录</button></template>
      <template v-else><div class="anonymous-status"><span class="status-dot"></span><span>匿名体验</span><strong>剩余 {{ account.remaining }} 次</strong></div><p v-if="account.remaining === 0" class="sidebar-login-hint">登录后可继续评审，已生成的项目仍会保留。</p></template>
    </footer>
  </aside>
  <div v-if="editing" class="account-dialog-backdrop">
    <section class="account-dialog panel" role="dialog" aria-modal="true" :aria-label="deleting ? '确认删除项目' : '管理项目'">
      <h2>{{ deleting ? '删除项目？' : '管理项目' }}</h2>
      <template v-if="!deleting"><form @submit.prevent="saveName"><label>项目名称<input v-model="rename" required maxlength="64" /></label><button :disabled="saving">保存名称</button></form><button :disabled="saving" @click="deleting = true">删除项目…</button></template>
      <template v-else><p>删除“{{ editing.name }}”及其所有报告和版本，同时清除本浏览器的对应草稿。此操作不能从界面撤销。</p><button :disabled="saving" @click="deleteProject">确认删除项目</button></template>
      <p v-if="actionError" role="alert">{{ actionError }}</p><button :disabled="saving" @click="editing = null; actionError = ''">取消</button>
    </section>
  </div>
  <p v-if="feedback" class="sidebar-feedback" role="status">{{ feedback }}</p>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { account, logout, loadSession } from '@/stores/account';
import { http, toErrorMessage } from '@/api/http';
import { useReviewStore } from '@/stores/reviewStore';
import { getReview } from '@/api/reviewApi';
import { newReviewAnimationRequest } from '@/stores/newReviewAnimation';
interface Project { projectId: string; name: string; latestVersion: number }
const route = useRoute(), router = useRouter(), store = useReviewStore();
const opened = ref(window.innerWidth > 760), items = ref<Project[]>([]), total = ref(0), page = ref(1), search = ref(''), appliedSearch = ref(''), loading = ref(false), error = ref('');
const editing = ref<Project | null>(null), rename = ref(''), deleting = ref(false), saving = ref(false), actionError = ref(''), feedback = ref(''), leaving = ref(false);
const inferredProject = ref('');
const currentProject = computed(() => String(route.params.groupId || inferredProject.value || ''));
let contextRequest = 0;
watch(() => [route.fullPath, account.identityKey], async () => {
  const run = ++contextRequest; inferredProject.value = '';
  const id = route.params.sourceId || route.params.id || route.query.result;
  if (!id || !/^\d+$/.test(String(id))) return;
  try { const review = await getReview(Number(id)); if (run === contextRequest) inferredProject.value = review.ideaGroupId; } catch { }
}, { immediate: true });
const loginTarget = computed(() => ({ path: '/account/login', query: route.path.startsWith('/account/') ? {} : { returnTo: route.fullPath } }));
const registerTarget = computed(() => ({ ...loginTarget.value, path: '/account/register' }));
let generation = 0;
async function fetchProjects(more = false) {
  const request = ++generation; loading.value = true; error.value = ''; const nextPage = more ? page.value + 1 : 1;
  try { const r = await http.get('/project-directory', { params: { search: appliedSearch.value, page: nextPage } }); if (request !== generation) return; const d = r.data.data; items.value = more ? [...items.value, ...d.items] : d.items; total.value = d.total; page.value = nextPage; }
  catch (e) { if (request === generation) error.value = toErrorMessage(e); } finally { if (request === generation) loading.value = false; }
}
function searchProjects() { appliedSearch.value = search.value; fetchProjects(); }
function closeMobile() { if (window.innerWidth <= 760) opened.value = false; }
async function openNewReview() {
  closeMobile();
  await router.push('/');
  await nextTick();
  newReviewAnimationRequest.value++;
}
async function refresh() { await fetchProjects(); await loadSession().catch(() => {}); }
async function saveName() { if (!editing.value) return; saving.value = true; actionError.value = ''; try { await http.patch(`/project-directory/${editing.value.projectId}`, { name: rename.value }); editing.value = null; feedback.value = '项目名称已保存'; await fetchProjects(); window.dispatchEvent(new Event('project-renamed')); } catch (e) { actionError.value = toErrorMessage(e); } finally { saving.value = false; } }
async function deleteProject() { if (!editing.value) return; saving.value = true; actionError.value = ''; const id = editing.value.projectId; try { await http.delete(`/project-directory/${id}`); for (const k of Object.keys(localStorage)) if (k.startsWith(`czspig-draft-v2:${account.identityKey}:${id}:`)) localStorage.removeItem(k); editing.value = null; store.$reset(); await router.replace('/projects'); await fetchProjects(); feedback.value = '项目、报告及版本已删除，对应草稿已清除'; } catch (e) { actionError.value = toErrorMessage(e); } finally { saving.value = false; } }
async function exit() { leaving.value = true; try { await logout(); window.location.replace('/'); } catch (e) { error.value = toErrorMessage(e); leaving.value = false; } }
watch(() => account.identityKey, () => { items.value = []; editing.value = null; search.value = appliedSearch.value = ''; store.$reset(); fetchProjects(); });
onMounted(() => { fetchProjects(); window.addEventListener('projects-changed', refresh); });
onUnmounted(() => window.removeEventListener('projects-changed', refresh));
</script>
