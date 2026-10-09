<template>
  <section class="workbench-shell">
    <header class="page-heading">
      <div>
        <p class="eyebrow">猪猪产品毒舌官 · {{ result ? '评审结果' : source ? '迭代草稿' : '新建评审' }}</p>
        <h1 v-if="result || source">{{ name }}</h1>
        <h1 v-else class="typing-title" :aria-label="reviewTitle">
          <span class="typing-title-space" aria-hidden="true">{{ reviewTitle }}</span>
          <span class="typing-title-text" aria-hidden="true">{{ visibleTitle }}</span>
        </h1>
        <p v-if="source">来源 V{{ source.versionNo }} · 待生成 V{{ nextVersion }} 草稿</p>
      </div>
      <RouterLink v-if="source" class="secondary-button" :to="{ name: 'review-detail', params: { id: source.id }, query: { from: route.query.from } }">返回 V{{ source.versionNo }} 报告</RouterLink>
    </header>
    <div v-if="initializing" class="loading-state">正在恢复编辑上下文…</div>
    <p v-else-if="loadError" class="inline-error" role="alert">{{ loadError }}</p>
    <template v-else-if="result">
      <section class="latest-docket summary-panel" aria-label="生成结果">
        <p class="eyebrow">{{ name }} · {{ result.providerName === 'mock' ? '模拟评审' : '产品评审' }}</p>
        <h2>V{{ result.versionNo }} 报告已生成</h2>
        <p>{{ result.oneLineVerdict }}</p>
        <div class="page-toolbar">
          <RouterLink class="primary-button" :to="{ name: 'review-detail', params: { id: result.id } }">查看完整报告</RouterLink>
          <RouterLink class="secondary-button" :to="{ name: 'project-workbench', params: { groupId: result.ideaGroupId } }">项目工作台</RouterLink>
          <RouterLink class="secondary-button" :to="{ name: 'iteration-draft', params: { sourceId: result.id } }">继续优化</RouterLink>
        </div>
        <p>继续优化会进入草稿编辑，提交后才生成新报告。</p>
        <RouterLink class="text-button" to="/">新建评审</RouterLink>
      </section>
      <p v-if="cleanupError" class="inline-error" role="alert">{{ cleanupError }}</p>
    </template>
    <template v-else>
      <p class="draft-status" :class="{ 'form-error': saveError }" role="status">{{ saveError || savedMessage }}</p>
      <p v-if="source">提交时按项目最新版本生成下一报告，编号以提交结果为准。</p>
      <ReviewInputForm :key="formKey" :loading="submitting" :initial-content="initial.content"
        :initial-mode="initial.mode" :initial-roast-level="initial.roastLevel"
        :idea-group-id="source?.ideaGroupId" :parent-review-id="source?.id"
        @draft-change="saveChanges" @submit="submit" />
      <p v-if="submitError" class="inline-error" role="alert">{{ submitError }}</p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import ReviewInputForm from '@/components/ReviewInputForm.vue';
import { createReview, getReview, getProjectWorkbench } from '@/api/reviewApi';
import { toErrorMessage } from '@/api/http';
import { buildNextDraft } from '@/utils/developmentPackage';
import { draftKey, readSavedDraft, writeSavedDraft, projectDisplayName } from '@/utils/experience';
import type { CreateReviewPayload, ReviewDetailResponse } from '@/types/review';
import { newReviewAnimationRequest } from '@/stores/newReviewAnimation';

const reviewTitle = '把产品想法变成可验证的方案';
const visibleTitle = ref(reviewTitle);
let titleTimer: ReturnType<typeof setInterval> | undefined;
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
function stopTitleAnimation() { clearInterval(titleTimer); titleTimer = undefined; }
function onMotionChange() { if (reducedMotion.matches) { stopTitleAnimation(); visibleTitle.value = reviewTitle; } }
reducedMotion.addEventListener('change', onMotionChange);
watch(newReviewAnimationRequest, () => {
  stopTitleAnimation();
  visibleTitle.value = reviewTitle;
  if (route.name !== 'home' || route.query.result || reducedMotion.matches) return;
  visibleTitle.value = '';
  const letters = Array.from(reviewTitle);
  let count = 0;
  titleTimer = setInterval(() => {
    visibleTitle.value = letters.slice(0, ++count).join('');
    if (count >= letters.length) stopTitleAnimation();
  }, 100);
});
onUnmounted(() => { stopTitleAnimation(); reducedMotion.removeEventListener('change', onMotionChange); });

const route = useRoute();
const router = useRouter();
const source = ref<ReviewDetailResponse | null>(null);
const result = ref<ReviewDetailResponse | null>(null);
const initial = ref<CreateReviewPayload>({ content: '', mode: 'SHARP_PM', roastLevel: 2 });
const name = ref('产品想法项目');
const nextVersion = ref(2);
const formKey = ref(0);
const initializing = ref(false);
const submitting = ref(false);
const loadError = ref('');
const submitError = ref('');
const saveError = ref('');
const cleanupError = ref('');
const savedMessage = ref('');
let storageKey = '';
let generation = 0;

async function initialize() {
  const run = ++generation;
  initializing.value = true;
  source.value = null;
  result.value = null;
  storageKey = '';
  loadError.value = submitError.value = saveError.value = savedMessage.value = '';
  try {
    let parent: ReviewDetailResponse | null = null;
    if (route.name === 'iteration-draft') {
      parent = await getReview(Number(route.params.sourceId));
      const project = await getProjectWorkbench(parent.ideaGroupId);
      const title = await projectDisplayName(project);
      if (run !== generation) return;
      name.value = title;
      nextVersion.value = Math.max(project.latestVersionNo, parent.versionNo) + 1;
    }
    if (route.query.result && route.name === 'home') {
      const generated = await getReview(Number(route.query.result));
      const title = await projectDisplayName(await getProjectWorkbench(generated.ideaGroupId));
      if (run !== generation) return;
      name.value = title;
      result.value = generated;
      return;
    }
    if (run !== generation) return;
    source.value = parent;
    initial.value = { content: parent ? buildNextDraft(parent) : '', mode: parent?.mode || 'SHARP_PM', roastLevel: parent?.roastLevel || 2 };
    try {
      storageKey = draftKey(parent?.ideaGroupId, parent?.id);
      const restored = readSavedDraft(storageKey);
      if (restored) initial.value = restored;
      writeSavedDraft(storageKey, initial.value);
      savedMessage.value = restored ? '草稿已恢复并保存，返回后可继续编辑。' : '草稿已保存，返回后可继续编辑。';
    } catch {
      saveError.value = '草稿保存或恢复失败，请复制当前输入备份；返回或刷新可能丢失内容。';
    }
    formKey.value++;
  } catch (error) {
    if (run === generation) loadError.value = toErrorMessage(error);
  } finally {
    if (run === generation) initializing.value = false;
  }
}
function saveChanges(payload: CreateReviewPayload) {
  try {
    if (!storageKey) throw new Error('草稿存储不可用');
    writeSavedDraft(storageKey, payload);
    saveError.value = '';
    savedMessage.value = '草稿已保存，返回后可继续编辑。';
  } catch {
    saveError.value = '草稿保存失败，请复制当前输入备份；返回或刷新可能丢失内容。';
  }
}
async function submit(payload: CreateReviewPayload) {
  if (submitting.value) return;
  submitting.value = true;
  submitError.value = '';
  saveChanges(payload);
  const submittedKey = storageKey;
  const submittedSource = source.value;
  const submittedPath = route.fullPath;
  try {
    const generated = await createReview({ ...payload, ideaGroupId: submittedSource?.ideaGroupId, parentReviewId: submittedSource?.id });
    try { localStorage.removeItem(submittedKey); }
    catch { cleanupError.value = '报告已生成，但旧草稿未能清理，请勿重复提交。'; }
    if (route.fullPath === submittedPath) {
      await router.push({ name: 'home', query: { result: generated.id } });
    }
  } catch (error) {
    submitError.value = toErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
watch(() => route.fullPath, initialize, { immediate: true });
</script>
