<template>
  <div class="list-block">
    <h3>{{ title }}</h3>
    <ul>
      <li v-for="item in safeItems" :key="item">{{ item }}</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  title: string;
  items?: string[];
}>();

const safeItems = computed(() => props.items?.filter(Boolean) ?? []);
</script>
