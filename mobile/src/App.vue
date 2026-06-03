<template>
  <div v-if="showIntro" class="intro-screen">
    <div class="intro-mark">
      <div class="intro-chart">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
    <div class="intro-copy">
      <div class="intro-kicker">Fund Valuation</div>
      <h1>基金估值</h1>
      <p>查看基金净值估算、持仓涨跌和权重贡献。</p>
    </div>
  </div>

  <div v-else class="app-container" :class="{ dark: isDarkMode }">
    <!-- Header with market status -->
    <header class="header">
      <div class="header-title">
        <button v-if="activeTab === 'valuation' && !showList" class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
        <div>
          <h1>{{ headerTitle }}</h1>
          <div v-if="activeTab === 'valuation' && showList" class="subtitle">
            <span class="market-status" :class="isMarketHours ? 'open' : 'closed'">
              {{ isMarketHours ? '交易中' : '已休市' }}
            </span>
            <span v-if="generatedAtText" class="update-time">· {{ generatedAtText }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <!-- Overseas fund button -->
        <button
          v-if="activeTab === 'overseas'"
          class="header-btn icon-btn"
          @click="loadOverseasData"
          :disabled="overseasLoading"
          :class="{ spinning: overseasLoading }"
          title="刷新海外基金"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 4v6h-6M1 20v-6h6"></path>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
        </button>

        <!-- Dark mode toggle -->
        <button class="header-btn icon-btn theme-toggle" @click="toggleDarkMode" :title="isDarkMode ? '切换亮色模式' : '切换暗色模式'">
          <svg v-if="isDarkMode" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
          </svg>
        </button>

        <!-- Refresh button (only on valuation tab) -->
        <button v-if="activeTab === 'valuation'" class="header-btn icon-btn" @click="refresh" :disabled="loading" :class="{ spinning: loading }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M23 4v6h-6M1 20v-6h6"></path>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
        </button>
      </div>
    </header>

    <!-- Error banner -->
    <div v-if="activeTab === 'valuation' && error && funds.length > 0" class="snackbar">
      <span>{{ error }}</span>
      <button class="retry" @click="refresh">重试</button>
    </div>

    <main
      class="scroll-area"
      ref="scrollArea"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
    >
      <!-- News Tab -->
      <template v-if="activeTab === 'news'">
        <section class="insight-panel">
          <div class="section-heading">
            <div>
              <div class="panel-label">AI 产业链</div>
              <h3>最新资讯</h3>
            </div>
            <span>{{ newsGeneratedAtText || '等待更新' }}</span>
          </div>
          <div v-if="newsLoading && newsItems.length === 0" class="state-page news-state">
            <div class="spinner"></div>
            <div class="text">正在加载实时资讯...</div>
          </div>
          <div v-else-if="newsError && newsItems.length === 0" class="state-page news-state">
            <div class="text">{{ newsError }}</div>
            <button class="btn-retry" @click="loadNews">重试</button>
          </div>
          <div v-else-if="newsItems.length === 0" class="state-page news-state">
            <div class="text">暂无匹配资讯</div>
          </div>
          <div v-else class="news-list">
            <button v-for="item in newsItems" :key="item.url" class="news-item" @click="openNews(item)">
              <div class="news-title">{{ item.title }}</div>
              <div class="news-meta">
                <span>{{ item.source || '资讯' }}</span>
                <span>{{ newsTimeText(item) }}</span>
              </div>
            </button>
            <div v-if="newsError" class="news-error">{{ newsError }}</div>
          </div>
        </section>
      </template>

      <!-- Valuation Tab -->
      <template v-else-if="activeTab === 'valuation'">
        <!-- Pull to refresh indicator -->
        <div class="pull-indicator" :class="{ visible: pullDistance > 0 }" :style="{ height: pullDistance + 'px' }">
          <div class="pull-spinner" :class="{ ready: pullDistance >= 60 }">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6M1 20v-6h6"></path>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
            </svg>
          </div>
        </div>

        <!-- Skeleton loading -->
        <template v-if="loading && funds.length === 0">
          <div class="skeleton-card">
            <div class="skeleton-row"><div class="skeleton-block" style="width: 40%"></div></div>
            <div class="skeleton-row"><div class="skeleton-block" style="width: 70%"></div></div>
          </div>
          <div class="skeleton-card">
            <div class="skeleton-row"><div class="skeleton-block" style="width: 50%"></div></div>
            <div class="skeleton-row"><div class="skeleton-block" style="width: 60%"></div></div>
          </div>
          <div class="skeleton-card">
            <div class="skeleton-row"><div class="skeleton-block" style="width: 45%"></div></div>
            <div class="skeleton-row"><div class="skeleton-block" style="width: 80%"></div></div>
          </div>
        </template>

        <div v-else-if="error && funds.length === 0" class="state-page">
          <div style="font-size:40px;margin-bottom:8px;">&#x26A0;</div>
          <div class="text">{{ error }}</div>
          <button class="btn-retry" @click="refresh">重试</button>
        </div>

        <div v-else-if="funds.length === 0" class="state-page">
          <div style="font-size:40px;margin-bottom:8px;">&#x1F4CA;</div>
          <div class="text">暂无基金数据</div>
          <div style="font-size:12px;color:#999;margin-top:6px;">请检查后端服务和基金数据文件</div>
        </div>

        <template v-else>
          <!-- Search and Filter Bar -->
          <div class="search-filter-bar">
            <div class="search-box">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input v-model="searchQuery" type="text" placeholder="搜索基金代码或名称" />
              <button v-if="searchQuery" class="clear-btn" @click="searchQuery = ''">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"></path>
                </svg>
              </button>
            </div>
            <div class="filter-actions">
              <button class="filter-btn" :class="{ active: sortBy === 'change' }" @click="toggleSort('change')">
                <span>涨跌幅</span>
                <svg v-if="sortBy === 'change'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path v-if="sortOrder === 'desc'" d="M6 9l6 6 6-6"></path>
                  <path v-else d="M18 15l-6-6-6 6"></path>
                </svg>
              </button>
              <button class="filter-btn star-btn" :class="{ active: showFavoritesOnly }" @click="toggleFavorites">
                <svg viewBox="0 0 24 24" :fill="showFavoritesOnly ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                </svg>
                <span>{{ showFavoritesOnly ? '已关注' : '关注' }}</span>
              </button>
            </div>
          </div>

          <!-- Fund List -->
          <FundList
            v-if="showList"
            :funds="filteredFunds"
            :selected-code="selectedCode"
            :favorites="favorites"
            @select="onFundSelect"
            @toggle-favorite="toggleFavorite"
          />
          <FundDetail
            v-else-if="selectedDetail"
            :detail="selectedDetail"
            :is-favorite="isFavorite(selectedCode)"
            @holding-click="onHoldingClick"
            @toggle-favorite="toggleFavorite(selectedCode)"
          />
        </template>
      </template>

      <!-- Profile Tab -->
      <template v-else-if="activeTab === 'profile'">
        <section class="profile-panel">
          <div class="profile-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <h2>基金估值</h2>
          <p>当前为本地演示账户，数据来自估值接口。</p>
        </section>

        <section class="stats-grid">
          <div class="stat-card">
            <div class="stat-value rise">{{ risingFunds }}</div>
            <div class="stat-label">上涨基金</div>
          </div>
          <div class="stat-card">
            <div class="stat-value fall">{{ fallingFunds }}</div>
            <div class="stat-label">下跌基金</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ favorites.length }}</div>
            <div class="stat-label">关注基金</div>
          </div>
        </section>

        <div class="profile-list">
          <div class="list-item">
            <span>接口状态</span>
            <strong :class="error ? 'fall' : 'rise'">{{ error ? '异常' : '正常' }}</strong>
          </div>
          <div class="list-item">
            <span>轮询频率</span>
            <strong>60 秒</strong>
          </div>
          <div class="list-item">
            <span>交易时段</span>
            <strong>{{ isMarketHours ? '进行中' : '已休市' }}</strong>
          </div>
        </div>
      </template>

      <!-- Overseas Tab -->
      <template v-else-if="activeTab === 'overseas'">
        <!-- Time Period Info Banner -->
        <div class="overseas-banner" v-if="timePeriodInfo">
          <div class="banner-content">
            <div class="banner-left">
              <span class="period-badge" :class="timePeriodInfo.period === 'US_MARKET_OPEN' ? 'open' : 'closed'">
                {{ timePeriodInfo.period === 'US_MARKET_OPEN' ? '美股盘中' : '美股已收盘' }}
              </span>
              <p class="period-desc">{{ timePeriodInfo.periodDescription }}</p>
            </div>
            <div class="banner-right">
              <div class="time-display">
                <span class="time-label">北京时间</span>
                <span class="time-value">{{ timePeriodInfo.chinaTime }}</span>
              </div>
              <div class="time-display">
                <span class="time-label">美东时间</span>
                <span class="time-value">{{ timePeriodInfo.usTime }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="overseasLoading && overseasFunds.length === 0" class="state-page">
          <div class="spinner"></div>
          <div class="text">正在加载海外基金数据...</div>
        </div>

        <!-- Error State -->
        <div v-else-if="overseasError && overseasFunds.length === 0" class="state-page">
          <div style="font-size:40px;margin-bottom:8px;">&#x26A0;</div>
          <div class="text">{{ overseasError }}</div>
          <button class="btn-retry" @click="loadOverseasData">重试</button>
        </div>

        <!-- Empty State -->
        <div v-else-if="overseasFunds.length === 0" class="state-page">
          <div style="font-size:40px;margin-bottom:8px;">&#x1F30D;</div>
          <div class="text">暂无海外基金数据</div>
          <div style="font-size:12px;color:#999;margin-top:6px;">请检查后端服务和基金配置</div>
        </div>

        <!-- Overseas Fund List -->
        <template v-else>
          <div class="overseas-funds-list">
            <div
              v-for="fund in overseasFunds"
              :key="fund.fundCode"
              class="overseas-fund-card"
              @click="selectOverseasFund(fund)"
            >
              <div class="fund-main">
                <div class="fund-info">
                  <span class="fund-tag">QDII</span>
                  <h3 class="fund-name">{{ fund.fundName }}</h3>
                  <p class="fund-code">{{ fund.fundCode }}</p>
                </div>
                <div class="fund-change" :class="pctClass(fund.estimatedChangePct)">
                  <span class="change-value">{{ formatPct(fund.estimatedChangePct) }}</span>
                  <span class="change-label">估算涨跌</span>
                </div>
              </div>
              <div class="fund-contribution">
                <div class="contrib-item rise">
                  <span class="contrib-label">美股贡献</span>
                  <span class="contrib-value">{{ formatPct(fund.usContribution) }}</span>
                </div>
                <div class="contrib-item">
                  <span class="contrib-label">时间状态</span>
                  <span class="contrib-value small">{{ fund.timePeriodDescription }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Selected Fund Detail -->
          <OverseasFundDetail
            v-if="selectedOverseasFund"
            :fund="selectedOverseasFund"
            @close="selectedOverseasFund = null"
          />
        </template>
      </template>
    </main>

    <!-- Bottom Navigation with SVG icons -->
    <nav class="bottom-nav" aria-label="主导航">
      <button :class="{ active: activeTab === 'valuation' }" @click="switchTab('valuation')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <rect x="3" y="3" width="7" height="7"></rect>
          <rect x="14" y="3" width="7" height="7"></rect>
          <rect x="14" y="14" width="7" height="7"></rect>
          <rect x="3" y="14" width="7" height="7"></rect>
        </svg>
        A股估值
      </button>
      <button :class="{ active: activeTab === 'overseas' }" @click="switchTab('overseas')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="2" y1="12" x2="22" y2="12"></line>
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
        </svg>
        海外估值
      </button>
      <button :class="{ active: activeTab === 'news' }" @click="switchTab('news')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M4 11a9 9 0 0 1 9 9"></path>
          <path d="M4 4a16 16 0 0 1 16 16"></path>
          <circle cx="5" cy="19" r="1"></circle>
        </svg>
        资讯
      </button>
    </nav>

    <HoldingSheet
      v-if="selectedHolding"
      :holding="selectedHolding"
      :fund-name="currentFundName"
      @close="selectedHolding = null"
    />

    <NewsArticlePage
      v-if="showArticleView && articleViewItem"
      :item="articleViewItem"
      :fallback-time="newsGeneratedAtText"
      @close="showArticleView = false"
    />

    <!-- Exit hint toast -->
    <div v-if="showExitToast" class="exit-toast">
      再滑动一次退出应用
    </div>
  </div>
</template>

<script>
import { App as CapacitorApp } from '@capacitor/app'
import FundList from './components/FundList.vue'
import FundDetail from './components/FundDetail.vue'
import HoldingSheet from './components/HoldingSheet.vue'
import OverseasFundDetail from './components/OverseasFundDetail.vue'
import NewsArticlePage from './components/NewsArticlePage.vue'
import { NEWS_API, SNAPSHOT_API, OVERSEAS_API, OVERSEAS_TIME_PERIOD_API, readJsonResponse } from './utils/api.js'
import { normalizeSnapshot } from './utils/snapshot.js'
import { normalizeOverseasFunds, normalizeTimePeriod } from './utils/overseas.js'
import { normalizeNewsItems } from './utils/news.js'
import { formatPct, pctClass } from './utils/formatters.js'

const POLL_INTERVAL = 60000
const DEFAULT_NEWS_QUERY = 'AI 半导体 PCB 光模块'
const FAVORITES_KEY = 'fund_favorites'
const DARK_MODE_KEY = 'fund_dark_mode'

export default {
  components: { FundList, FundDetail, HoldingSheet, OverseasFundDetail, NewsArticlePage },
  data() {
    return {
      showIntro: true,
      activeTab: 'valuation',
      funds: [],
      details: {},
      generatedAt: 0,
      loading: true,
      error: '',
      showList: true,
      selectedCode: '',
      selectedHolding: null,
      pollTimer: null,
      marketTimer: null,
      timePeriodTimer: null,
      introTimer: null,
      newsItems: [],
      newsGeneratedAt: 0,
      newsLoading: false,
      newsError: '',
      newsPollTimer: null,
      newsRequestInFlight: false,
      articleViewItem: null,
      showArticleView: false,
      // Overseas funds
      overseasFunds: [],
      overseasLoading: false,
      overseasError: '',
      selectedOverseasFund: null,
      timePeriodInfo: null,
      overseasPollTimer: null,
      overseasRequestInFlight: false,
      snapshotRequestInFlight: false,
      // New features
      searchQuery: '',
      sortBy: '',
      sortOrder: 'desc',
      favorites: [],
      showFavoritesOnly: false,
      pullDistance: 0,
      pullStartY: 0,
      touchStartX: 0,
      touchStartY: 0,
      isPulling: false,
      isHorizontalSwipe: false,
      swipeBackDistance: 0,
      swipeHandled: false,
      exitPending: false,
      exitTimer: null,
      showExitToast: false,
      isMarketHours: false,
      scrollElement: null,
      isDarkMode: false,
    }
  },
  computed: {
    selectedDetail() {
      return this.details[this.selectedCode] || null
    },
    currentFundName() {
      const f = this.funds.find(f => f.fundCode === this.selectedCode)
      return f?.fundName || this.selectedDetail?.fundName || ''
    },
    generatedAtText() {
      return this.formatTimestamp(this.generatedAt)
    },
    newsGeneratedAtText() {
      return this.formatTimestamp(this.newsGeneratedAt)
    },
    headerTitle() {
      if (this.activeTab === 'news') return '资讯'
      if (this.activeTab === 'profile') return '我的'
      if (this.activeTab === 'overseas') return '海外估值'
      return this.showList ? '基金估值' : (this.currentFundName || '基金详情')
    },
    risingFunds() {
      return this.funds.filter(f => Number(f.estimatedChangePct) >= 0).length
    },
    fallingFunds() {
      return this.funds.filter(f => Number(f.estimatedChangePct) < 0).length
    },
    filteredFunds() {
      let result = [...this.funds]

      // Filter by search
      if (this.searchQuery) {
        const q = this.searchQuery.toLowerCase()
        result = result.filter(f =>
          f.fundCode.toLowerCase().includes(q) ||
          f.fundName.toLowerCase().includes(q)
        )
      }

      // Filter by favorites
      if (this.showFavoritesOnly) {
        result = result.filter(f => this.favorites.includes(f.fundCode))
      }

      // Sort
      if (this.sortBy === 'change') {
        result.sort((a, b) => {
          const diff = Number(a.estimatedChangePct) - Number(b.estimatedChangePct)
          return this.sortOrder === 'desc' ? -diff : diff
        })
      }

      return result
    },
  },
  mounted() {
    this.loadData()
    this.startPolling()
    this.updateMarketStatus()
    this.loadFavorites()
    this.loadDarkMode()
    this.loadTimePeriod()
    this.introTimer = setTimeout(() => {
      this.showIntro = false
    }, 1000)

    this.marketTimer = setInterval(() => this.updateMarketStatus(), POLL_INTERVAL)
    this.timePeriodTimer = setInterval(() => this.loadTimePeriod(), POLL_INTERVAL)
  },
  unmounted() {
    this.stopPolling()
    this.stopNewsPolling()
    this.stopOverseasPolling()
    if (this.marketTimer) {
      clearInterval(this.marketTimer)
      this.marketTimer = null
    }
    if (this.timePeriodTimer) {
      clearInterval(this.timePeriodTimer)
      this.timePeriodTimer = null
    }
    if (this.introTimer) {
      clearTimeout(this.introTimer)
      this.introTimer = null
    }
  },
  methods: {
    formatPct,
    pctClass,
    updateMarketStatus() {
      const now = new Date()
      const chinaHour = (now.getUTCHours() + 8) % 24
      const day = now.getUTCDay()
      const isWeekday = day >= 1 && day <= 5
      this.isMarketHours = isWeekday && chinaHour >= 9 && chinaHour < 17
    },
    loadFavorites() {
      try {
        const saved = localStorage.getItem(FAVORITES_KEY)
        this.favorites = saved ? JSON.parse(saved) : []
      } catch {
        this.favorites = []
      }
    },
    saveFavorites() {
      localStorage.setItem(FAVORITES_KEY, JSON.stringify(this.favorites))
    },
    loadDarkMode() {
      try {
        const saved = localStorage.getItem(DARK_MODE_KEY)
        this.isDarkMode = saved === 'true'
        this.applyDarkMode()
      } catch {
        this.isDarkMode = false
      }
    },
    applyDarkMode() {
      if (this.isDarkMode) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    },
    toggleDarkMode() {
      this.isDarkMode = !this.isDarkMode
      localStorage.setItem(DARK_MODE_KEY, String(this.isDarkMode))
      this.applyDarkMode()
    },
    toggleFavorite(code) {
      const idx = this.favorites.indexOf(code)
      if (idx >= 0) {
        this.favorites.splice(idx, 1)
      } else {
        this.favorites.push(code)
      }
      this.saveFavorites()
    },
    isFavorite(code) {
      return this.favorites.includes(code)
    },
    toggleFavorites() {
      this.showFavoritesOnly = !this.showFavoritesOnly
    },
    toggleSort(field) {
      if (this.sortBy === field) {
        this.sortOrder = this.sortOrder === 'desc' ? 'asc' : 'desc'
      } else {
        this.sortBy = field
        this.sortOrder = 'desc'
      }
    },
    switchTab(tab) {
      this.activeTab = tab
      if (tab !== 'valuation') {
        this.showList = true
      }
      if (tab === 'news' && this.newsItems.length === 0 && !this.newsLoading) {
        this.loadNews()
      }
      if (tab === 'news') {
        this.startNewsPolling()
      } else {
        this.stopNewsPolling()
      }
      if (tab === 'overseas') {
        this.loadOverseasData()
        this.startOverseasPolling()
      } else {
        this.stopOverseasPolling()
      }
    },
    goValuation() {
      this.activeTab = 'valuation'
      this.showList = true
    },
    async loadData() {
      if (this.snapshotRequestInFlight) return
      this.snapshotRequestInFlight = true
      this.loading = this.funds.length === 0
      this.error = ''
      try {
        const res = await fetch(SNAPSHOT_API)
        const data = normalizeSnapshot(await readJsonResponse(res))
        this.funds = data.funds
        this.details = data.details
        this.generatedAt = data.generatedAt
        if (!this.selectedCode && data.funds.length > 0) {
          this.selectedCode = data.funds[0].fundCode
        }
      } catch (e) {
        this.error = e.message || '加载失败'
      } finally {
        this.loading = false
        this.snapshotRequestInFlight = false
      }
    },
    refresh() {
      this.loadData()
    },
    async loadNews() {
      if (this.newsRequestInFlight) return
      this.newsRequestInFlight = true
      this.newsLoading = true
      this.newsError = ''
      try {
        const params = new URLSearchParams({
          query: DEFAULT_NEWS_QUERY,
          limit: '20',
        })
        const res = await fetch(`${NEWS_API}?${params.toString()}`)
        const data = await readJsonResponse(res)
        this.newsItems = normalizeNewsItems(data.items)
        this.newsGeneratedAt = data.generated_at || data.generatedAt || 0
        this.newsError = data.error || ''
      } catch (e) {
        this.newsError = e.message || '资讯加载失败'
      } finally {
        this.newsLoading = false
        this.newsRequestInFlight = false
      }
    },
    newsTimeText(item) {
      return item?.publishedAt || item?.published_at || this.newsGeneratedAtText || '刚刚更新'
    },
    openNews(item) {
      this.articleViewItem = item
      this.showArticleView = true
    },
    formatTimestamp(value) {
      if (!value) return ''
      const ts = value < 1000000000000 ? value * 1000 : value
      const d = new Date(ts)
      const pad = n => String(n).padStart(2, '0')
      return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    onFundSelect(code) {
      this.selectedCode = code
      this.showList = false
    },
    onHoldingClick(holding) {
      this.selectedHolding = holding
    },
    goBack() {
      this.showList = true
    },
    async smartBack() {
      // 1. Article view → close
      if (this.showArticleView && this.articleViewItem) {
        this.showArticleView = false
        return
      }
      // 2. Overseas fund detail → close
      if (this.selectedOverseasFund) {
        this.selectedOverseasFund = null
        return
      }
      // 3. Holding sheet → close
      if (this.selectedHolding) {
        this.selectedHolding = null
        return
      }
      // 4. Fund detail → back to list
      if (!this.showList) {
        this.showList = true
        return
      }
      // 5. Top level → double-swipe to exit
      if (this.exitPending) {
        // Second swipe: exit app
        if (this.exitTimer) {
          clearTimeout(this.exitTimer)
          this.exitTimer = null
        }
        this.exitPending = false
        this.showExitToast = false
        try {
          await CapacitorApp.exitApp()
        } catch {
          // Fallback for browser/dev environment
          try { window.close() } catch {}
        }
        return
      }
      // First swipe: show toast hint
      this.exitPending = true
      this.showExitToast = true
      this.exitTimer = setTimeout(() => {
        this.exitPending = false
        this.showExitToast = false
        this.exitTimer = null
      }, 2000)
    },
    startPolling() {
      this.pollTimer = setInterval(async () => {
        try {
          const res = await fetch(SNAPSHOT_API)
          const data = normalizeSnapshot(await readJsonResponse(res))
          this.funds = data.funds
          this.details = data.details
          this.generatedAt = data.generatedAt
        } catch (_) {}
      }, POLL_INTERVAL)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    startNewsPolling() {
      if (this.newsPollTimer) return
      this.newsPollTimer = setInterval(() => {
        this.loadNews()
      }, POLL_INTERVAL)
    },
    stopNewsPolling() {
      if (this.newsPollTimer) {
        clearInterval(this.newsPollTimer)
        this.newsPollTimer = null
      }
    },
    // Overseas funds methods
    async loadTimePeriod() {
      try {
        const res = await fetch(OVERSEAS_TIME_PERIOD_API)
        this.timePeriodInfo = normalizeTimePeriod(await readJsonResponse(res))
      } catch (e) {
        console.warn('Failed to load time period:', e)
      }
    },
    async loadOverseasData() {
      if (this.overseasRequestInFlight) return
      this.overseasRequestInFlight = true
      this.overseasLoading = this.overseasFunds.length === 0
      this.overseasError = ''
      try {
        const res = await fetch(OVERSEAS_API)
        const data = await readJsonResponse(res)
        this.overseasFunds = normalizeOverseasFunds(data)
        this.syncSelectedOverseasFund()
      } catch (e) {
        this.overseasError = e.message || '加载失败'
      } finally {
        this.overseasLoading = false
        this.overseasRequestInFlight = false
      }
    },
    selectOverseasFund(fund) {
      this.selectedOverseasFund = fund
    },
    syncSelectedOverseasFund() {
      if (!this.selectedOverseasFund) return
      const current = this.overseasFunds.find(f => f.fundCode === this.selectedOverseasFund.fundCode)
      this.selectedOverseasFund = current || null
    },
    startOverseasPolling() {
      if (this.overseasPollTimer) return
      this.overseasPollTimer = setInterval(() => {
        this.loadOverseasData()
      }, POLL_INTERVAL)
    },
    stopOverseasPolling() {
      if (this.overseasPollTimer) {
        clearInterval(this.overseasPollTimer)
        this.overseasPollTimer = null
      }
    },
    // Touch handlers: swipe back + pull to refresh
    onTouchStart(e) {
      const touch = e.touches[0]
      this.touchStartX = touch.clientX
      this.touchStartY = touch.clientY
      this.pullStartY = touch.clientY
      this.isHorizontalSwipe = false
      this.isPulling = false
      this.swipeBackDistance = 0
      this.swipeHandled = false

      // Enable pull-to-refresh only on valuation tab list
      if (this.activeTab === 'valuation' && this.showList && !this.loading) {
        const el = this.$refs.scrollArea
        if (el && el.scrollTop === 0) {
          this.isPulling = true
        }
      }
    },
    onTouchMove(e) {
      const touch = e.touches[0]
      const deltaX = touch.clientX - this.touchStartX
      const deltaY = touch.clientY - this.touchStartY
      const isHorizontal = Math.abs(deltaX) > 14 && Math.abs(deltaX) > Math.abs(deltaY) * 1.2

      if (!this.isHorizontalSwipe && isHorizontal) {
        this.isHorizontalSwipe = true
      }

      if (this.isHorizontalSwipe) {
        e.preventDefault()
        this.swipeBackDistance = Math.abs(deltaX)

        // Trigger back when swiped past 30% of screen width (one shot)
        const threshold = window.innerWidth * 0.3
        if (this.swipeBackDistance >= threshold && !this.swipeHandled) {
          this.swipeHandled = true
          this.smartBack()
        }
        return
      }

      // Vertical pull-to-refresh
      if (!this.isPulling) return
      if (deltaY > 0) {
        e.preventDefault()
        this.pullDistance = Math.min(deltaY * 0.5, 100)
      }
    },
    onTouchEnd() {
      const wasPulling = this.isPulling
      this.isPulling = false
      if (wasPulling && this.pullDistance >= 60) {
        this.refresh()
      }
      this.pullDistance = 0
      this.pullStartY = 0
      this.touchStartX = 0
      this.touchStartY = 0
      this.isHorizontalSwipe = false
      this.swipeBackDistance = 0
      // swipeHandled resets on next touchStart
    },
  },
}
</script>
