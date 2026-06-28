<template>
  <div class="pixel-pig" :class="[`pixel-pig--${size}`, `pixel-pig--${pose}`]" aria-hidden="true">
    <div class="pixel-pig__ears">
      <span></span>
      <span></span>
    </div>
    <div class="pixel-pig__head">
      <span class="pixel-pig__eye pixel-pig__eye--left"></span>
      <span class="pixel-pig__eye pixel-pig__eye--right"></span>
      <span class="pixel-pig__glasses"></span>
      <span class="pixel-pig__snout"></span>
    </div>
    <div class="pixel-pig__body">
      <span class="pixel-pig__tie"></span>
      <span class="pixel-pig__clipboard"></span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    size?: 'small' | 'medium' | 'large';
    pose?: 'badge' | 'manager';
  }>(),
  {
    size: 'medium',
    pose: 'manager',
  },
);
</script>
