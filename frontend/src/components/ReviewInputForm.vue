<template>
  <form class="review-form" @submit.prevent="handleSubmit">
    <div class="form-header">
      <div>
        <p class="eyebrow">产品评审工作台</p>
        <h1>把想法交给产品毒打</h1>
      </div>
      <button class="icon-button" type="button" title="填入示例" @click="fillSample">
        <Sparkles :size="18" />
      </button>
    </div>

    <label class="field">
      <span>产品想法 / 需求材料</span>
      <textarea
        v-model.trim="form.content"
        maxlength="5000"
        placeholder="例如：我想做一个面向独立开发者的 AI 产品评审工具，输入产品想法后输出伪需求风险、MVP 范围和开发 Prompt。"
      />
    </label>

    <div class="form-row">
      <span class="counter" :class="{ danger: form.content.length > 4800 }">{{ form.content.length }}/5000</span>
      <span v-if="localError" class="form-error">{{ localError }}</span>
    </div>

    <fieldset class="segmented-field">
      <legend>评审模式</legend>
      <div class="segmented-control">
        <button
          v-for="option in modeOptions"
          :key="option.value"
          type="button"
          :class="{ active: form.mode === option.value }"
          @click="form.mode = option.value"
        >
          <component :is="option.icon" :size="17" />
          <span>{{ option.label }}</span>
        </button>
      </div>
    </fieldset>

    <fieldset class="segmented-field">
      <legend>吐槽强度</legend>
      <div class="level-control" role="group" aria-label="吐槽强度">
        <button
          v-for="level in levelOptions"
          :key="level.value"
          type="button"
          :class="{ active: form.roastLevel === level.value }"
          @click="form.roastLevel = level.value"
        >
          {{ level.label }}
        </button>
      </div>
    </fieldset>

    <button class="primary-button" type="submit" :disabled="loading">
      <LoaderCircle v-if="loading" class="spin" :size="18" />
      <Send v-else :size="18" />
      <span>{{ loading ? '生成中' : '开始评审' }}</span>
    </button>
  </form>
</template>

<script setup lang="ts">
import { reactive, ref, type Component } from 'vue';
import { BriefcaseBusiness, LoaderCircle, Send, ShieldQuestion, Sparkles, Target } from '@lucide/vue';

import type { CreateReviewPayload, ReviewMode } from '@/types/review';

defineProps<{
  loading: boolean;
}>();

const emit = defineEmits<{
  submit: [payload: CreateReviewPayload];
}>();

const form = reactive<CreateReviewPayload>({
  content: '',
  mode: 'SHARP_PM',
  roastLevel: 2,
});

const localError = ref('');

const modeOptions: Array<{ value: ReviewMode; label: string; icon: Component }> = [
  { value: 'MENTOR', label: '导师', icon: Target },
  { value: 'SHARP_PM', label: '毒舌 PM', icon: ShieldQuestion },
  { value: 'CLIENT', label: '甲方', icon: BriefcaseBusiness },
];

const levelOptions = [
  { value: 1, label: '温和' },
  { value: 2, label: '直接' },
  { value: 3, label: '毒舌' },
];

function handleSubmit() {
  if (form.content.length < 10) {
    localError.value = '至少输入 10 个字符';
    return;
  }
  localError.value = '';
  emit('submit', { ...form });
}

function fillSample() {
  form.content =
    '我想做一个面向独立开发者和小团队的 AI 产品毒打工具。用户输入产品想法后，系统从伪需求、冷启动、功能冗余、MVP 范围和开发 Prompt 角度输出评审报告，帮助他们在真正开发前砍掉不必要功能。';
}
</script>
