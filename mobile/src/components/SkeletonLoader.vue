<template>
  <div class="skeleton-loader">
    <div class="skeleton-row" v-for="i in rows" :key="i">
      <div class="skeleton-block" :style="{ width: widths[i % widths.length] }"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 },
  widths: { type: Array, default: () => ['60%', '80%', '45%', '70%'] },
})
</script>

<style scoped>
.skeleton-loader {
  padding: 12px 0;
}

.skeleton-row {
  margin-bottom: 12px;
}

.skeleton-block {
  height: 16px;
  background: linear-gradient(
    90deg,
    var(--skeleton-bg, #f0f0f0) 25%,
    var(--skeleton-highlight, #e0e0e0) 50%,
    var(--skeleton-bg, #f0f0f0) 75%
  );
  background-size: 200% 100%;
  border-radius: 4px;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Card skeleton variant */
.skeleton-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 10px;
}

.skeleton-card .skeleton-row {
  margin-bottom: 8px;
}

.skeleton-card .skeleton-row:last-child {
  margin-bottom: 0;
}

/* Panel skeleton variant */
.skeleton-panel {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 12px;
}

/* Dark mode support */
:global(.dark) .skeleton-block {
  --skeleton-bg: #2a2a2a;
  --skeleton-highlight: #3a3a3a;
}
</style>
