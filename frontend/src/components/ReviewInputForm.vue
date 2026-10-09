<template>
  <form class="review-form draft-board" @submit.prevent="handleSubmit">
    <div class="draft-header">
      <div>
        <p class="eyebrow">Draft Workspace</p>
        <h2>产品想法评估工作台</h2>
      </div>
      <button class="stamp-button" type="button" title="填入示例" @click="applyExample(exampleIdeas[0])">
        <Sparkles :size="16" />
        <span>试个样例</span>
      </button>
    </div>

    <div class="input-guide" aria-label="输入提示">
      <div>
        <h3>写清 5 件事，毒蛇官才喷得准</h3>
        <p>目标用户、真实痛点、使用场景、现有方案差异、商业模式。不用写长论文，把关键事实交代清楚就行。</p>
      </div>
      <div class="guide-tags">
        <button v-for="tag in guideTags" :key="tag.label" type="button" @click="insertGuide(tag.text)">
          {{ tag.label }}
        </button>
      </div>
      <div class="example-ideas" aria-label="示例产品 idea">
        <button v-for="idea in exampleIdeas" :key="idea.title" type="button" @click="applyExample(idea)">
          {{ idea.title }}
        </button>
      </div>
    </div>

    <label class="draft-field">
      <span>把你的产品 idea 摊在桌上</span>
      <textarea
        v-model="form.content"
        maxlength="5000"
        placeholder="例如：我想做一个面向大三大四学生的 AI 简历优化工具，用户在投递前不知道简历和 JD 哪里不匹配..."
      />
    </label>

    <div class="form-row">
      <span v-if="localError" class="form-error">{{ localError }}</span>
      <span v-else class="quality-hint" :class="`quality-hint--${inputQuality.status}`">{{ inputQuality.message }}</span>
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
            :aria-pressed="form.mode === option.value"
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
          <span>毒舌程度</span>
        </legend>
        <div class="level-control annotation-control" role="group" aria-label="毒舌程度">
          <button
            v-for="level in levelOptions"
            :key="level.value"
            type="button"
            :class="{ active: form.roastLevel === level.value }"
            :aria-pressed="form.roastLevel === level.value"
            :title="level.description"
            @click="form.roastLevel = level.value"
          >
            {{ level.label }}
          </button>
        </div>
      </fieldset>

      <button class="primary-button start-review" type="submit" :disabled="loading">
        <LoaderCircle v-if="loading" class="spin" :size="18" />
        <ArrowRight v-else :size="18" />
        <span>{{ loading ? '正在生成报告…' : parentReviewId ? '提交草稿并生成新报告' : '生成 V1 评审报告' }}</span>
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, type Component } from 'vue';
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
  initialMode?: ReviewMode;
  initialRoastLevel?: number;
}>();

const emit = defineEmits<{
  submit: [payload: CreateReviewPayload];
  draftChange: [payload: CreateReviewPayload];
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
  { value: 1, label: '温和', description: '专业提醒，少阴阳怪气' },
  { value: 2, label: '犀利', description: '默认模式，专业但嘴毒' },
  { value: 3, label: '地狱', description: '更狠，但仍然给建设性建议' },
];

const guideTags = [
  { label: '目标用户', text: '目标用户：' },
  { label: '真实痛点', text: '真实痛点：' },
  { label: '使用场景', text: '使用场景：' },
  { label: '差异点', text: '和现有方案不同的是：' },
  { label: '商业模式', text: '商业模式：' },
];

const exampleIdeas = [
  {
    title: '校园二手交易小程序',
    content:
      '我想做一个校园二手交易小程序，目标用户是同一所大学里的学生，尤其是毕业季要处理教材、宿舍用品和电子配件的人。痛点是微信群消息容易刷屏、闲鱼同城不够精准、线下交付信任成本高。使用场景是学生在宿舍或教学楼附近发布二手物品，买家按校区和品类筛选后联系卖家线下交付。和现有方案不同的是只做校内身份、校区距离和快速成交闭环。商业模式可以从置顶曝光、毕业季服务包或校园商家合作开始验证。',
  },
  {
    title: 'AI 简历优化工具',
    content:
      '我想做一个 AI 简历优化工具，目标用户是正在投递实习和校招岗位的大学生、转岗求职者。痛点是他们不知道简历和目标 JD 哪里不匹配，也不知道怎么把经历改成有证据链的表达。使用场景是用户上传简历并粘贴岗位 JD，系统给出匹配度诊断、逐段修改建议和修改理由。和通用 AI 聊天不同的是输出必须围绕具体岗位关键词和面试反馈验证。商业模式初期可以按单次优化收费，后续再考虑求职包或院校合作。',
  },
  {
    title: '宠物健康管理 App',
    content:
      '我想做一个宠物健康管理 App，目标用户是养猫养狗的新手主人。痛点是疫苗、驱虫、体重、症状记录分散在备忘录和聊天记录里，就医前很难整理清楚。使用场景是主人日常记录宠物档案、提醒疫苗驱虫，在异常症状出现时导出一份就医摘要给兽医参考。和现有备忘录不同的是围绕宠物健康时间线和提醒闭环设计，但不做在线诊断。商业模式可以从高级提醒、家庭多宠物档案和宠物医院合作切入。',
  },
];

const inputQuality = computed(() => {
  const content = form.content.trim();
  if (content.length < 30) {
    return { status: 'weak', message: '这个想法还太短，毒蛇官只能闻到一点味儿' };
  }
  if (!hasAny(content, ['目标用户', '用户是', '面向', '给', '学生', '求职者', '主人', '甲方', '客户'])) {
    return { status: 'warn', message: '再补一句目标用户是谁，别让毒蛇官隔空猜人群' };
  }
  if (!hasAny(content, ['使用场景', '场景', '当', '在', '投递', '毕业季', '就医前', '日常'])) {
    return { status: 'warn', message: '再补一个使用场景，说明用户什么时候会真的打开它' };
  }
  return { status: 'ready', message: '信息够了，可以开喷' };
});

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

watch(
  () => [form.content, form.mode, form.roastLevel] as const,
  () => emit('draftChange', { ...form, ideaGroupId: props.ideaGroupId, parentReviewId: props.parentReviewId }),
);
watch(
  () => props.initialMode,
  (value) => { if (value) form.mode = value; },
  { immediate: true },
);
watch(
  () => props.initialRoastLevel,
  (value) => { if (value) form.roastLevel = value; },
  { immediate: true },
);

function handleSubmit() {
  if (form.content.trim().length < 10) {
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
  localError.value = '';
}

function applyExample(idea: { title: string; content: string }) {
  form.content = idea.content;
  form.mode = 'SHARP_PM';
  form.roastLevel = 2;
  localError.value = '';
}

function hasAny(content: string, keywords: string[]) {
  return keywords.some((keyword) => content.includes(keyword));
}
</script>

<style scoped>
.example-ideas {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.example-ideas button {
  min-height: 30px;
  border: 1px solid rgba(71, 105, 95, 0.34);
  padding: 0 10px;
  background: var(--success-soft);
  color: var(--success);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
}

.quality-hint {
  border-left: 4px solid var(--border-dark);
  padding-left: 10px;
  font-size: 13px;
  font-weight: 900;
}

.quality-hint--weak {
  border-color: var(--danger);
  color: var(--danger);
}

.quality-hint--warn {
  border-color: var(--warning);
  color: var(--warning);
}

.quality-hint--ready {
  border-color: var(--success);
  color: var(--success);
}
</style>
