# 手机端资讯内嵌阅读器设计方案

## 概述

将手机端「资讯」Tab 的新闻详情阅读从「底部弹出面板 + 跳转外部链接」改为「全屏应用内嵌文章阅读」，用户点击新闻后直接在应用内查看文章富文本内容。

## 当前问题

- 新闻列表展示标题、来源、时间
- 点击后弹出底部面板（`NewsDetailSheet`），只有占位文字和原文 URL
- 「查看原文」链接跳转外部浏览器（东方财富/新浪财经）
- 用户离开应用体验，且无法自定义阅读样式

## 目标

- 点击新闻 → 全屏文章阅读页，在应用内显示正文
- 保留原文的排版格式（段落、图片、粗体、列表等）
- 支持加载/错误/重试状态
- 内容缓存（Redis，TTL 24h），避免重复抓取
- 兼容现有暗色模式

## 后端改动

### 1. 新增依赖

`pom.xml` 添加 Jsoup（HTML 解析与内容提取）：

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.19.1</version>
</dependency>
```

### 2. 新增 DTO

`src/main/java/org/dsb/fundvaluation/dto/NewsContentResponse.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| `url` | String | 原文链接 |
| `title` | String | 文章标题 |
| `content` | String | 清洗后的正文 HTML |
| `source` | String | 来源站点 |
| `fetchedAt` | long | 抓取时间戳 |

### 3. NewsService 新增方法

**`getArticleContent(String url) → NewsContentResponse`**

逻辑：
1. 计算 URL 的 MD5 作为缓存 key
2. 查询 Redis（key: `news:content:<MD5>`）：
   - 缓存命中 → 反序列化直接返回
   - 缓存未命中 → 继续
3. Jsoup 抓取 URL（UA: Mozilla/5.0，超时 5 秒）
4. 多选择器尝试定位正文区域（按优先级）：
   - `.article-body`, `.news-content`, `.detail-content`
   - `#content`, `.Body`, `.main-content`, `article`
   - 回退：`body`
5. 清洗：移除 `script`、`style`、`iframe`、广告类元素（`.ad`, `.advertisement` 等）
6. 构造 `NewsContentResponse`：
   - `title` = 页面 `<title>`
   - `content` = 选取元素的 `.html()`
   - `source` = 从 URL 推断或页面中提取
   - `fetchedAt` = 当前时间戳
7. 写入 Redis 缓存，TTL = 24 小时
8. 返回结果

### 4. 新增 API 端点

**`GET /api/news/content?url=<encoded_url>`**

```java
@GetMapping("/news/content")
public NewsContentResponse getNewsContent(@RequestParam("url") String url) {
    return newsService.getArticleContent(url);
}
```

无需认证，从服务端发出请求，不受前端 CORS 限制。

### 5. 缓存说明

- **Key 格式**: `news:content:` + URL 的 MD5 十六进制小写
- **TTL**: 24 小时（`TimeUnit.DAYS`）
- **失效场景**: 新闻内容更新 → 等待 TTL 到期或手动清除缓存
- **存储量**: 假设 20 篇文章 × 平均 20KB ≈ 400KB，Redis 压力极小

## 前端改动

### 1. 新建组件 `NewsArticlePage.vue`

位置：`mobile/src/components/NewsArticlePage.vue`

#### Props

| Prop | 类型 | 必填 | 说明 |
|------|------|------|------|
| `item` | Object | 是 | 新闻项 `{ title, url, source, publishedAt }` |

#### Emits

| 事件 | 说明 |
|------|------|
| `close` | 用户点击返回/关闭按钮 |

#### Data/State

| 状态 | 类型 | 说明 |
|------|------|------|
| `content` | String | 文章正文 HTML（后端返回） |
| `loading` | Boolean | 是否正在加载 |
| `error` | String | 错误信息（空=无错误） |
| `articleTitle` | String | 文章标题（优先使用后端返回的） |
| `articleSource` | String | 文章来源 |
| `fetchedAt` | Number | 抓取时间 |

#### 状态视图

| 状态 | 显示 |
|------|------|
| 加载中 | 骨架屏：标题占位条 + 3-4 段文字占位条 + 图片占位矩形 |
| 加载成功 | 文章标题（大号） + 来源·时间 + 分隔线 + HTML 正文 |
| 加载失败 | 错误图标 + 错误描述 + 「重试」按钮 + 「返回」按钮 |
| 空内容 | 同加载失败风格，提示「暂未获取到文章内容」 |

#### 样式

- `position: fixed; inset: 0; z-index: 1000` — 全屏覆盖
- `overflow-y: auto` — 可滚动阅读
- 背景色与当前主题一致（暗色/亮色）
- 文章正文字号 16px，行高 1.7，段落间距 1em
- 图片 `max-width: 100%; height: auto; border-radius: 8px`
- 链接使用主题色

#### 安全

- 使用 `v-html` 渲染（内容来自自有后端，已清洗）
- 后端已移除 `<script>` 和 `<iframe>` 等危险元素

### 2. App.vue 改动

#### 修改点清单

| 项目 | 原值 | 新值 |
|------|------|------|
| 导入 | `NewsDetailSheet` | `NewsArticlePage` |
| 组件注册 | `NewsDetailSheet` | `NewsArticlePage` |
| data | `selectedNewsItem: null` | `articleViewItem: null` |
| data | — | `showArticleView: false` |
| 方法 `openNews(item)` | `this.selectedNewsItem = item` | `this.articleViewItem = item; this.showArticleView = true` |
| template | `<NewsDetailSheet v-if="selectedNewsItem" ... />` | `<NewsArticlePage v-if="showArticleView && articleViewItem" :item="articleViewItem" @close="showArticleView = false" />` |

### 3. 删除文件

- `mobile/src/components/NewsDetailSheet.vue`（已被替代）
- `mobile/src/utils/news.js`（仅用于 `normalizeNewsItems`，检查是否仍有引用）

### 4. 网络请求

`NewsArticlePage` 内部调用 API（直接用 `fetch` 而非 Vuex/Pinia）：
```
GET /api/news/content?url=${encodeURIComponent(item.url)}
```

沿用 `mobile/src/utils/api.js` 中的 `readJsonResponse` 工具函数（可复用或内联）。

## 数据流

```
用户点击新闻列表项
  → App.vue: openNews(item)
    → showArticleView = true, articleViewItem = item
    → 渲染 <NewsArticlePage>
      → onMounted: fetch /api/news/content?url=...
        → 后端: NewsService.getArticleContent(url)
          → Redis 有缓存? → 直接返回
          → 无缓存: Jsoup 抓取 → 提取 → 清洗 → 写入 Redis → 返回
        → 前端: 收到 NewsContentResponse
          → loading=false, content=响应.content
      → 异常时: loading=false, error=错误描述
```

## 错误处理

| 场景 | 后端处理 | 前端显示 |
|------|---------|---------|
| URL 参数为空 | 400 Bad Request | "缺少文章链接" |
| 抓取超时 | 504 Gateway Timeout | "文章加载超时，请重试" |
| 页面不存在 (404) | 返回空内容 | "暂未获取到文章内容" |
| Redis 异常 | 跳过缓存，实时抓取 | — |
| 网络断开 | — | "网络连接失败，请检查网络" + 重试按钮 |

## 测试要点

1. **后端单元测试**: `NewsServiceTest` 添加 `getArticleContent` 的测试用例（mock Jsoup）
2. **前端组件测试**: `NewsArticlePage` 的加载/成功/失败状态
3. **缓存验证**: 重复请求同一 URL 应命中缓存
4. **跨源测试**: 东方财富、新浪财经等不同来源的文章提取效果
5. **暗色模式**: 文章页在暗色/亮色下均可正常阅读

## 注意事项

- Jsoup 内容提取选择器需要持续维护（目标网站改版时需适配）
- 建议保留「查看文章原文」选项（但改为在应用内嵌显示，而非跳转浏览器）
- 缓存 TTL 不宜过短，文章内容变化频率低