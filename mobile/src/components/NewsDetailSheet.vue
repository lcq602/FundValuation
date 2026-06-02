<template>
  <div class="sheet-overlay" @click="$emit('close')">
    <article class="news-sheet" @click.stop>
      <div class="sheet-handle"></div>

      <header class="news-sheet-header">
        <div class="news-sheet-meta">
          <span>{{ item.source || '资讯' }}</span>
          <span>{{ displayTime }}</span>
        </div>
        <h2>{{ item.title }}</h2>
      </header>

      <div class="news-sheet-body">
        <p>
          来源已收录到本地资讯流，可结合基金估值和持仓变化一起查看。
        </p>
        <p class="news-url">{{ item.url }}</p>
      </div>

      <div class="news-sheet-actions">
        <a class="origin-link" :href="item.url" target="_blank" rel="noreferrer">查看原文</a>
        <button class="close-btn" @click="$emit('close')">关闭</button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
  fallbackTime: { type: String, default: '' },
})

defineEmits(['close'])

const displayTime = computed(() => props.item.publishedAt || props.fallbackTime || '刚刚更新')
</script>
