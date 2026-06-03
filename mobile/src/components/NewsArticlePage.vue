<template>
  <div class="news-article-page">
    <!-- Header -->
    <header class="article-header">
      <button class="back-btn" @click="$emit('close')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </button>
      <h1 class="header-title">资讯</h1>
    </header>

    <!-- Content area -->
    <div class="article-scroll" ref="scrollRef">
      <!-- Loading skeleton -->
      <template v-if="loading">
        <div class="skeleton-wrapper">
          <div class="skeleton-block skeleton-title-line"></div>
          <div class="skeleton-block skeleton-meta-line"></div>
          <div class="skeleton-divider"></div>
          <div class="skeleton-block skeleton-text-line" style="width: 100%"></div>
          <div class="skeleton-block skeleton-text-line" style="width: 95%"></div>
          <div class="skeleton-block skeleton-text-line" style="width: 60%"></div>
          <div class="skeleton-block skeleton-img-placeholder"></div>
          <div class="skeleton-block skeleton-text-line" style="width: 100%"></div>
          <div class="skeleton-block skeleton-text-line" style="width: 80%"></div>
          <div class="skeleton-block skeleton-text-line" style="width: 45%"></div>
        </div>
      </template>

      <!-- Error state -->
      <div v-else-if="error" class="state-content">
        <div class="state-icon">&#x26A0;</div>
        <p class="state-text">{{ error }}</p>
        <div class="state-actions">
          <button class="btn-primary" @click="loadContent">重试</button>
          <button class="btn-secondary" @click="$emit('close')">返回</button>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="!content" class="state-content">
        <p class="state-text">暂未获取到文章内容</p>
        <div class="state-actions">
          <button class="btn-secondary" @click="$emit('close')">返回</button>
        </div>
      </div>

      <!-- Article content -->
      <article v-else class="article-body">
        <h2 class="article-title">{{ articleTitle }}</h2>
        <div class="article-meta">
          <span class="meta-source">{{ articleSource }}</span>
          <span class="meta-dot">·</span>
          <span class="meta-time">{{ displayTime }}</span>
        </div>
        <div class="article-divider"></div>
        <div class="article-content" v-html="content"></div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { readJsonResponse } from '../utils/api'

const CONTENT_API = '/api/news/content'

const props = defineProps({
  item: { type: Object, required: true },
  fallbackTime: { type: String, default: '' },
})

defineEmits(['close'])

const content = ref('')
const loading = ref(true)
const error = ref('')
const articleTitle = ref('')
const articleSource = ref('')
const fetchedAt = ref(0)

const scrollRef = ref(null)

const displayTime = computed(() => {
  const ts = fetchedAt.value || props.item?.publishedAt || 0
  if (ts) {
    const value = ts < 1000000000000 ? ts * 1000 : ts
    const d = new Date(value)
    const pad = n => String(n).padStart(2, '0')
    return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  if (props.fallbackTime) return props.fallbackTime
  return ''
})

async function loadContent() {
  if (!props.item?.url) {
    error.value = '缺少文章链接'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  content.value = ''
  try {
    const res = await fetch(`${CONTENT_API}?url=${encodeURIComponent(props.item.url)}`)
    const data = await readJsonResponse(res)
    if (data.content) {
      content.value = data.content
      articleTitle.value = data.title || props.item.title || ''
      articleSource.value = data.source || props.item.source || '资讯'
      fetchedAt.value = data.fetchedAt || 0
    } else {
      error.value = '暂未获取到文章内容'
    }
  } catch (e) {
    error.value = e.message === 'Failed to fetch' ? '网络连接失败，请检查网络' : (e.message || '文章加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadContent()
})
</script>

<style scoped>
.news-article-page {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  /* CSS variables for theming */
  --bg-primary: #ffffff;
  --text-primary: #1a1a1a;
  --border-color: #e8e8e8;
  --text-secondary: #8c8c8c;
  --accent-color: #1677ff;
}

/* Dark mode overrides */
:global(.dark) .news-article-page {
  --bg-primary: #12121a;
  --text-primary: #e8e8e8;
  --border-color: #2a2a3a;
  --text-secondary: #8a8a9a;
  --accent-color: #4dabf7;
}

/* Header */
.article-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  padding-top: calc(12px + var(--safe-top, 0px));
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  min-height: calc(56px + var(--safe-top, 0px));
  flex-shrink: 0;
}

.back-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.back-btn:active {
  background: var(--border-color);
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

/* Scrollable content */
.article-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
}

/* ==================== Loading Skeleton ==================== */
.skeleton-wrapper {
  padding: 24px 20px;
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

.skeleton-title-line {
  width: 75%;
  height: 28px;
  margin-bottom: 12px;
}

.skeleton-meta-line {
  width: 40%;
  height: 14px;
  margin-bottom: 20px;
}

.skeleton-divider {
  height: 1px;
  background: var(--border-color);
  margin-bottom: 20px;
}

.skeleton-text-line {
  margin-bottom: 14px;
}

.skeleton-img-placeholder {
  height: 180px;
  margin-bottom: 14px;
  border-radius: 8px;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Dark mode skeleton */
:global(.dark) .skeleton-block {
  --skeleton-bg: #2a2a3a;
  --skeleton-highlight: #3a3a4a;
}

/* ==================== State Pages ==================== */
.state-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.state-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.state-text {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  line-height: 1.6;
  margin-bottom: 20px;
}

.state-actions {
  display: flex;
  gap: 12px;
}

.btn-primary {
  padding: 10px 28px;
  background: var(--accent-color);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:active {
  opacity: 0.8;
}

.btn-secondary {
  padding: 10px 28px;
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-secondary:active {
  opacity: 0.7;
}

/* ==================== Article Content ==================== */
.article-body {
  padding: 24px 20px calc(40px + var(--safe-bottom, 0px));
}

.article-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.meta-source {
  color: var(--accent-color);
  font-weight: 600;
}

.meta-dot {
  color: var(--text-secondary);
}

.meta-time {
  color: var(--text-secondary);
}

.article-divider {
  height: 1px;
  background: var(--border-color);
  margin-bottom: 20px;
}

/* v-html content styling */
.article-content :deep(p) {
  margin-bottom: 1em;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
  margin: 16px auto;
}

.article-content :deep(a) {
  color: var(--accent-color);
  text-decoration: none;
}

.article-content :deep(a:hover) {
  text-decoration: underline;
}

.article-content :deep(h2) {
  font-size: 20px;
  font-weight: 700;
  margin: 1.2em 0 0.6em;
  color: var(--text-primary);
}

.article-content :deep(h3) {
  font-size: 17px;
  font-weight: 700;
  margin: 1em 0 0.5em;
  color: var(--text-primary);
}

.article-content :deep(blockquote) {
  border-left: 3px solid var(--accent-color);
  background: var(--bg-secondary, #f5f5f5);
  margin: 1em 0;
  padding: 12px 16px;
  border-radius: 0 6px 6px 0;
  color: var(--text-secondary);
  font-style: italic;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  padding-left: 20px;
  margin-bottom: 1em;
  color: var(--text-primary);
}

.article-content :deep(li) {
  margin-bottom: 0.4em;
  font-size: 16px;
  line-height: 1.8;
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  font-size: 14px;
}

.article-content :deep(th),
.article-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
  color: var(--text-primary);
}

.article-content :deep(th) {
  font-weight: 700;
  background: var(--bg-secondary, #f5f5f5);
}

.article-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 1.2em 0;
}

/* Dark mode additional styles */
:global(.dark) .article-content :deep(blockquote) {
  --bg-secondary: #1e1e2e;
}

:global(.dark) .article-content :deep(th) {
  --bg-secondary: #1e1e2e;
}

/* Responsive background for blockquote/th in non-dark mode */
.article-content :deep(blockquote) {
  --bg-secondary: #f5f5f5;
}

.article-content :deep(th) {
  --bg-secondary: #f5f5f5;
}
</style>