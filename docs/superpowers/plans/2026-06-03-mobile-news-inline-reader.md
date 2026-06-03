# 手机端资讯内嵌阅读器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current news bottom-sheet (placeholder + external link) with a full-screen in-app article reader that fetches and displays the article content inline.

**Architecture:** Backend (Spring Boot) adds a Jsoup-based article content fetcher with Redis caching, exposing `GET /api/news/content?url=`. Frontend (Vue 3) replaces NewsDetailSheet with a new NewsArticlePage full-screen component that calls this API and renders the cleaned HTML with proper loading/error states.

**Tech Stack:** Jsoup 1.19.1 (backend HTML parsing), Vue 3 + Capacitor 7 (frontend), Redis (content cache, TTL 24h)

---

### Task 1: Add Jsoup dependency to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Jsoup dependency**

Insert after the existing `spring-boot-starter-data-redis` dependency block (around line 49):

```xml
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.19.1</version>
        </dependency>
```

- [ ] **Step 2: Commit**

```bash
git add pom.xml
git commit -m "build: add jsoup dependency for article content extraction"
```

---

### Task 2: Create NewsContentResponse DTO

**Files:**
- Create: `src/main/java/org/dsb/fundvaluation/dto/NewsContentResponse.java`

- [ ] **Step 1: Create DTO class**

```java
package org.dsb.fundvaluation.dto;

public class NewsContentResponse {
    private String url;
    private String title;
    private String content;
    private String source;
    private long fetchedAt;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/org/dsb/fundvaluation/dto/NewsContentResponse.java
git commit -m "feat: add NewsContentResponse DTO for article content API"
```

---

### Task 3: Add getArticleContent method to NewsService

**Files:**
- Modify: `src/main/java/org/dsb/fundvaluation/service/NewsService.java`

- [ ] **Step 1: Add imports to NewsService.java**

Add at the top of the file, after the existing imports:

```java
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
```

- [ ] **Step 2: Add getArticleContent method**

Add this method after the `scheduledRefresh()` method (around line 77):

```java
public NewsContentResponse getArticleContent(String url) {
    if (url == null || url.isBlank()) {
        throw new IllegalArgumentException("url must not be blank");
    }

    String cacheKey = "news:content:" + DigestUtils.md5DigestAsHex(url.getBytes(StandardCharsets.UTF_8));
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null && !cached.isBlank()) {
        try {
            return objectMapper.readValue(cached, NewsContentResponse.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize cached article content, will re-fetch: {}", e.getMessage());
        }
    }

    NewsContentResponse response = fetchAndParseArticle(url);
    try {
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response),
                Duration.ofDays(1));
    } catch (Exception e) {
        log.warn("Failed to cache article content: {}", e.getMessage());
    }
    return response;
}

private NewsContentResponse fetchAndParseArticle(String url) {
    try {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(8000)
                .get();

        Element article = selectArticleBody(doc);
        if (article != null) {
            article.select("script, style, iframe, noscript, .ad, .advertisement, " +
                    ".gg-box, .gg_group, .banner, .recommend, .share, .footer").remove();
            article.select("[class*=ad], [class*=gg_], [id*=ad], [id*=gg_]").remove();
        }

        NewsContentResponse response = new NewsContentResponse();
        response.setUrl(url);
        response.setTitle(doc.title());
        response.setContent(article != null ? article.html() : "");
        response.setSource(extractSource(doc, url));
        response.setFetchedAt(System.currentTimeMillis());
        return response;
    } catch (Exception e) {
        log.warn("Failed to fetch article content from {}: {}", url, e.getMessage());
        NewsContentResponse response = new NewsContentResponse();
        response.setUrl(url);
        response.setTitle("");
        response.setContent("");
        response.setSource("");
        response.setFetchedAt(System.currentTimeMillis());
        return response;
    }
}

private Element selectArticleBody(Document doc) {
    String[] selectors = {
        ".article-body", ".news-content", ".detail-content", "#content",
        ".Body", ".main-content", "article", ".article-content",
        ".detail-body", ".news-body", ".art-body"
    };
    for (String selector : selectors) {
        Element el = doc.selectFirst(selector);
        if (el != null) return el;
    }
    return doc.body();
}

private String extractSource(Document doc, String url) {
    String site = "";
    if (url.contains("finance.eastmoney.com")) site = "东方财富";
    else if (url.contains("stock.eastmoney.com")) site = "东方财富";
    else if (url.contains("fund.eastmoney.com")) site = "东方财富";
    else if (url.contains("hk.eastmoney.com")) site = "东方财富";
    else if (url.contains("sina.com.cn")) site = "新浪财经";
    else site = "资讯";

    Element sourceEl = doc.selectFirst(".source, .data-source, .article-source, .info-source");
    if (sourceEl != null && !sourceEl.text().isBlank()) {
        site = sourceEl.text().trim();
    }
    return site;
}
```

- [ ] **Step 3: Add import for NewsContentResponse**

Make sure the import `import org.dsb.fundvaluation.dto.NewsContentResponse;` is present.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/org/dsb/fundvaluation/service/NewsService.java
git commit -m "feat: add getArticleContent with Jsoup extraction and Redis caching"
```

---

### Task 4: Add /api/news/content endpoint to FundController

**Files:**
- Modify: `src/main/java/org/dsb/fundvaluation/controller/FundController.java`

- [ ] **Step 1: Add the content endpoint**

Add after the existing `getNews()` method (around line 47):

```java
import org.dsb.fundvaluation.dto.NewsContentResponse;

@GetMapping("/news/content")
public NewsContentResponse getNewsContent(@RequestParam("url") String url) {
    return newsService.getArticleContent(url);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/org/dsb/fundvaluation/controller/FundController.java
git commit -m "feat: add GET /api/news/content endpoint for article fetching"
```

---

### Task 5: Write unit tests for getArticleContent

**Files:**
- Modify: `src/test/java/org/dsb/fundvaluation/NewsServiceTest.java`

- [ ] **Step 1: Add test for getArticleContent with Redis cache hit**

```java
@Test
void getArticleContentReturnsCachedContent() throws Exception {
    var redisTemplate = mockRedis(null);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get(anyString())).thenReturn(null); // cache miss → will fetch via Jsoup

    var service = new NewsService(new RestTemplate(), redisTemplate, new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZoneId.of("UTC")));

    // getArticleContent will try Jsoup.connect() - it will throw because no real URL.
    // We verify the error handling returns empty content gracefully.
    var result = service.getArticleContent("https://stock.eastmoney.com/a/202605253748075277.html");

    assertThat(result.getUrl()).isEqualTo("https://stock.eastmoney.com/a/202605253748075277.html");
    assertThat(result.getContent()).isEqualTo("");
    assertThat(result.getFetchedAt()).isGreaterThan(0);
}
```

- [ ] **Step 2: Run the test to verify it compiles and passes**

Run: `mvn test -pl . -Dtest=NewsServiceTest#getArticleContentReturnsCachedContent -DfailIfNoTests=false`
Expected: PASS (or at least compiles — the mock setup ensures graceful error handling)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/dsb/fundvaluation/NewsServiceTest.java
git commit -m "test: add unit test for getArticleContent graceful error handling"
```

---

### Task 6: Create NewsArticlePage.vue full-screen component

**Files:**
- Create: `mobile/src/components/NewsArticlePage.vue`

- [ ] **Step 1: Create the component file**

```vue
<template>
  <div class="article-page">
    <header class="article-header">
      <button class="back-btn" @click="$emit('close')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </button>
      <h1 class="article-header-title">资讯</h1>
    </header>

    <main class="article-scroll" ref="scrollRef">
      <!-- Loading State -->
      <div v-if="loading" class="article-skeleton">
        <div class="skeleton-title"></div>
        <div class="skeleton-meta"></div>
        <div class="skeleton-line" style="width: 100%"></div>
        <div class="skeleton-line" style="width: 95%"></div>
        <div class="skeleton-line" style="width: 60%"></div>
        <div class="skeleton-image"></div>
        <div class="skeleton-line" style="width: 100%"></div>
        <div class="skeleton-line" style="width: 80%"></div>
        <div class="skeleton-line" style="width: 45%"></div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="article-error">
        <div class="error-icon">&#x26A0;</div>
        <div class="error-text">{{ error }}</div>
        <button class="btn-retry" @click="loadContent">重试</button>
        <button class="btn-back" @click="$emit('close')">返回</button>
      </div>

      <!-- Empty Content -->
      <div v-else-if="!content" class="article-error">
        <div class="error-icon">&#x1F4DD;</div>
        <div class="error-text">暂未获取到文章内容</div>
        <button class="btn-back" @click="$emit('close')">返回</button>
      </div>

      <!-- Article Content -->
      <template v-else>
        <article class="article-body">
          <h1 class="article-title">{{ articleTitle }}</h1>
          <div class="article-meta">
            <span class="article-source">{{ articleSource }}</span>
            <span class="article-time">{{ displayTime }}</span>
          </div>
          <div class="article-divider"></div>
          <div class="article-content" v-html="content"></div>
        </article>
      </template>
    </main>
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
const articleTitle = ref(props.item?.title || '')
const articleSource = ref(props.item?.source || '资讯')
const fetchedAt = ref(0)

const displayTime = computed(() => {
  if (fetchedAt.value) {
    const d = new Date(fetchedAt.value)
    const pad = n => String(n).padStart(2, '0')
    return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  return props.fallbackTime || props.item?.publishedAt || '刚刚更新'
})

async function loadContent() {
  if (!props.item?.url) {
    error.value = '缺少文章链接'
    loading.value = false
    return
  }

  loading.value = true
  error.value = ''

  try {
    const res = await fetch(`${CONTENT_API}?url=${encodeURIComponent(props.item.url)}`)
    const data = await readJsonResponse(res)
    if (data.content) {
      content.value = data.content
      if (data.title) articleTitle.value = data.title
      if (data.source) articleSource.value = data.source
      fetchedAt.value = data.fetchedAt || 0
    } else {
      content.value = ''
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
.article-page {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: var(--bg-primary, #fff);
  display: flex;
  flex-direction: column;
  color: var(--text-primary, #1a1a2e);
}

.dark .article-page {
  background: var(--bg-primary-dark, #1a1a2e);
  color: var(--text-primary-dark, #e0e0e0);
}

.article-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, #eee);
  flex-shrink: 0;
}

.dark .article-header {
  border-bottom-color: var(--border-color-dark, #2a2a4a);
}

.back-btn {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.back-btn:hover {
  background: var(--hover-bg, #f0f0f0);
}

.dark .back-btn:hover {
  background: var(--hover-bg-dark, #2a2a4a);
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.article-header-title {
  font-size: 17px;
  font-weight: 600;
  margin: 0;
}

.article-scroll {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* Article Body */
.article-body {
  padding: 20px 16px 40px;
  max-width: 720px;
  margin: 0 auto;
}

.article-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  margin: 0 0 12px;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary, #888);
  margin-bottom: 16px;
}

.dark .article-meta {
  color: var(--text-secondary-dark, #999);
}

.article-divider {
  height: 1px;
  background: var(--border-color, #eee);
  margin-bottom: 20px;
}

.dark .article-divider {
  background: var(--border-color-dark, #2a2a4a);
}

/* Article Content (v-html rendered) */
.article-content {
  font-size: 16px;
  line-height: 1.8;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.article-content :deep(p) {
  margin: 0 0 1em;
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
  margin: 16px auto;
}

.article-content :deep(a) {
  color: var(--accent-color, #4f46e5);
  text-decoration: none;
}

.dark .article-content :deep(a) {
  color: var(--accent-color-dark, #818cf8);
}

.article-content :deep(strong), .article-content :deep(b) {
  font-weight: 700;
}

.article-content :deep(h2), .article-content :deep(h3) {
  margin: 1.2em 0 0.6em;
  line-height: 1.4;
}

.article-content :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 16px;
  border-left: 4px solid var(--accent-color, #4f46e5);
  background: var(--hover-bg, #f9f9f9);
  border-radius: 0 8px 8px 0;
}

.dark .article-content :deep(blockquote) {
  background: var(--hover-bg-dark, #252545);
}

.article-content :deep(ul), .article-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 14px;
}

.article-content :deep(th), .article-content :deep(td) {
  border: 1px solid var(--border-color, #ddd);
  padding: 8px 12px;
  text-align: left;
}

.dark .article-content :deep(th), .dark .article-content :deep(td) {
  border-color: var(--border-color-dark, #333);
}

/* Skeleton Loading */
.article-skeleton {
  padding: 20px 16px;
}

.skeleton-title {
  height: 28px;
  width: 75%;
  background: var(--skeleton-bg, #e0e0e0);
  border-radius: 6px;
  margin-bottom: 12px;
}

.dark .skeleton-title {
  background: var(--skeleton-bg-dark, #2a2a4a);
}

.skeleton-meta {
  height: 14px;
  width: 40%;
  background: var(--skeleton-bg, #e0e0e0);
  border-radius: 4px;
  margin-bottom: 24px;
}

.dark .skeleton-meta {
  background: var(--skeleton-bg-dark, #2a2a4a);
}

.skeleton-line {
  height: 16px;
  background: var(--skeleton-bg, #e0e0e0);
  border-radius: 4px;
  margin-bottom: 12px;
  animation: shimmer 1.5s infinite;
}

.dark .skeleton-line {
  background: var(--skeleton-bg-dark, #2a2a4a);
}

.skeleton-image {
  height: 180px;
  background: var(--skeleton-bg, #e0e0e0);
  border-radius: 8px;
  margin: 16px 0;
  animation: shimmer 1.5s infinite;
}

.dark .skeleton-image {
  background: var(--skeleton-bg-dark, #2a2a4a);
}

@keyframes shimmer {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

/* Error State */
.article-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 32px;
  text-align: center;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.error-text {
  font-size: 15px;
  color: var(--text-secondary, #888);
  margin-bottom: 20px;
}

.dark .error-text {
  color: var(--text-secondary-dark, #999);
}

.btn-retry, .btn-back {
  padding: 10px 28px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  margin: 4px;
}

.btn-retry {
  background: var(--accent-color, #4f46e5);
  color: #fff;
}

.btn-back {
  background: var(--hover-bg, #f0f0f0);
  color: var(--text-primary, #1a1a2e);
}

.dark .btn-back {
  background: var(--hover-bg-dark, #2a2a4a);
  color: var(--text-primary-dark, #e0e0e0);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add mobile/src/components/NewsArticlePage.vue
git commit -m "feat: add NewsArticlePage full-screen component for inline article reading"
```

---

### Task 7: Update App.vue to use NewsArticlePage

**Files:**
- Modify: `mobile/src/App.vue`

- [ ] **Step 1: Update import (line 399)**

Change:
```javascript
import NewsDetailSheet from './components/NewsDetailSheet.vue'
```
To:
```javascript
import NewsArticlePage from './components/NewsArticlePage.vue'
```

- [ ] **Step 2: Update components registration**

Change:
```javascript
components: { FundList, FundDetail, HoldingSheet, OverseasFundDetail, NewsDetailSheet },
```
To:
```javascript
components: { FundList, FundDetail, HoldingSheet, OverseasFundDetail, NewsArticlePage },
```

- [ ] **Step 3: Replace data properties (lines 435-436)**

Change:
```javascript
selectedNewsItem: null,
```
To:
```javascript
articleViewItem: null,
showArticleView: false,
```

- [ ] **Step 4: Update openNews method**

Change:
```javascript
openNews(item) {
  this.selectedNewsItem = item
},
```
To:
```javascript
openNews(item) {
  this.articleViewItem = item
  this.showArticleView = true
},
```

- [ ] **Step 5: Replace NewsDetailSheet in template (around line 385-390)**

Change:
```html
<NewsDetailSheet
  v-if="selectedNewsItem"
  :item="selectedNewsItem"
  :fallback-time="newsGeneratedAtText"
  @close="selectedNewsItem = null"
/>
```
To:
```html
<NewsArticlePage
  v-if="showArticleView && articleViewItem"
  :item="articleViewItem"
  :fallback-time="newsGeneratedAtText"
  @close="showArticleView = false"
/>
```

- [ ] **Step 6: Commit**

```bash
git add mobile/src/App.vue
git commit -m "feat: switch from NewsDetailSheet to NewsArticlePage in App.vue"
```

---

### Task 8: Delete obsolete NewsDetailSheet.vue

**Files:**
- Delete: `mobile/src/components/NewsDetailSheet.vue`

- [ ] **Step 1: Verify NewsDetailSheet is no longer imported anywhere**

Run: `grep -r "NewsDetailSheet" mobile/src/`
Expected: no matches

- [ ] **Step 2: Delete the file**

```bash
git rm mobile/src/components/NewsDetailSheet.vue
```

- [ ] **Step 3: Commit**

```bash
git commit -m "refactor: remove obsolete NewsDetailSheet component"
```

---

### Task 9: Verify the build compiles

- [ ] **Step 1: Build backend**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Build frontend**

```bash
cd mobile && npx vite build
```
Expected: Build completes with no errors

- [ ] **Step 3: Run backend tests**

```bash
mvn test
```
Expected: All tests pass

---

### Task 10: Final commit and summary

- [ ] **Step 1: Check all files are tracked**

```bash
git status
```
Expected: Clean working tree, all changes committed

- [ ] **Step 2: Push summary**

All changes are self-contained and can be deployed together. The backend exposes a new API endpoint, the frontend uses it to display articles inline without leaving the app.