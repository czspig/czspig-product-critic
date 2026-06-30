<template>
  <form class="review-form draft-board" @submit.prevent="handleSubmit">
    <div class="draft-header">
      <div>
        <p class="eyebrow">Draft Workspace</p>
        <h2>产品需求草稿</h2>
      </div>
      <button class="stamp-button" type="button" title="填入示例" @click="fillSample">
        <Sparkles :size="16" />
        <span>示例</span>
      </button>
    </div>

    <div class="input-guide" aria-label="输入提示">
      <div>
        <h3>怎么写，猪猪 PM 才能批得更准？</h3>
        <p>你可以按这 4 点写：目标用户是谁？用户现在遇到什么问题？你准备用什么产品方式解决？第一版最想验证什么？</p>
      </div>
      <div class="guide-tags">
        <button v-for="tag in guideTags" :key="tag.label" type="button" @click="insertGuide(tag.text)">
          {{ tag.label }}
        </button>
      </div>
    </div>

    <label class="draft-field">
      <span>把你的想法先摊在纸上</span>
      <textarea
        v-model.trim="form.content"
        maxlength="5000"
        placeholder="试着写清楚：你想服务谁？他们现在怎么解决？你的产品准备怎么做？第一版最想验证什么？"
      />
    </label>

    <div class="form-row">
      <span v-if="localError" class="form-error">{{ localError }}</span>
      <span class="counter" :class="{ danger: form.content.length > 4800 }">{{ form.content.length }}/5000 字</span>
    </div>

    <div class="config-strip">
      <fieldset class="segmented-field">
        <legend>
          <SlidersHorizontal :size="15" />
          <span>评审模式</span>
        </legend>
        <div class="segmented-control annotation-control">
          <button
            v-for="option in modeOptions"
            :key="option.value"
            type="button"
            :class="{ active: form.mode === option.value }"
            @click="form.mode = option.value"
          >
            <component :is="option.icon" :size="16" />
            <span>{{ option.label }}</span>
          </button>
        </div>
      </fieldset>

      <fieldset class="segmented-field">
        <legend>
          <Gauge :size="15" />
          <span>吐槽强度</span>
        </legend>
        <div class="level-control annotation-control" role="group" aria-label="吐槽强度">
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

      <button class="primary-button start-review" type="submit" :disabled="loading">
        <LoaderCircle v-if="loading" class="spin" :size="18" />
        <ArrowRight v-else :size="18" />
        <span>{{ loading ? '评审中' : '开始批注' }}</span>
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, ref, watch, type Component } from 'vue';
import {
  ArrowRight,
  BriefcaseBusiness,
  Gauge,
  LoaderCircle,
  ShieldQuestion,
  SlidersHorizontal,
  Sparkles,
  Target,
} from '@lucide/vue';

import type { CreateReviewPayload, ReviewMode } from '@/types/review';

const props = defineProps<{
  loading: boolean;
  initialContent?: string;
  ideaGroupId?: string;
  parentReviewId?: number;
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
  { value: 'MENTOR', label: '温和导师', icon: Target },
  { value: 'SHARP_PM', label: '毒舌 PM', icon: ShieldQuestion },
  { value: 'CLIENT', label: '甲方视角', icon: BriefcaseBusiness },
];

const levelOptions = [
  { value: 1, label: '温和' },
  { value: 2, label: '直接' },
  { value: 3, label: '毒舌' },
];

const guideTags = [
  { label: '目标用户', text: '目标用户：' },
  { label: '真实痛点', text: '真实痛点：' },
  { label: '现有替代方案', text: '现有替代方案：' },
  { label: '核心功能', text: '核心功能：' },
  { label: '第一版验证目标', text: '第一版验证目标：' },
];

watch(
  () => props.initialContent,
  (value) => {
    if (value) {
      form.content = value;
      localError.value = '';
    }
  },
  { immediate: true },
);

function handleSubmit() {
  if (form.content.length < 10) {
    localError.value = '至少输入 10 个字符，产品经理才有得批注。';
    return;
  }
  localError.value = '';
  emit('submit', {
    ...form,
    ideaGroupId: props.ideaGroupId,
    parentReviewId: props.parentReviewId,
  });
}

function insertGuide(text: string) {
  const prefix = form.content.trim() ? '\n' : '';
  form.content = `${form.content}${prefix}${text}`;
}

function fillSample() {
  form.content =
    '我想做一个面向独立开发者和小团队的 AI 产品评审工具。用户经常在产品想法还很模糊时就开始写代码，结果做了很多没人愿意用的功能。这个工具希望让用户输入目标用户、核心痛点和想做的功能后，输出伪需求风险、冷启动问题、MVP 范围和给开发工具的下一步 Prompt，帮助他们先砍掉不必要的功能。';
  form.mode = 'SHARP_PM';
  form.roastLevel = 2;
  localError.value = '';
}
</script>
