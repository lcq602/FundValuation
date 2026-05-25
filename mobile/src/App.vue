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

  <div v-else class="app-container">
    <header class="header">
      <div class="header-title">
        <button v-if="activeTab === 'valuation' && !showList" class="back-btn" @click="goBack">&larr;</button>
        <div>
          <h1>{{ headerTitle }}</h1>
          <div v-if="activeTab === 'valuation' && showList && generatedAtText" class="subtitle">
            最后更新 {{ generatedAtText }}
          </div>
        </div>
      </div>
      <div v-if="activeTab === 'valuation'" class="header-actions">
        <button class="header-btn" @click="refresh" :disabled="loading">&#x21bb;</button>
      </div>
    </header>

    <div v-if="activeTab === 'valuation' && error && funds.length > 0" class="snackbar">
      <span>{{ error }}</span>
      <button class="retry" @click="refresh">重试</button>
    </div>

    <main class="scroll-area">
      <template v-if="activeTab === 'news'">
        <section class="home-panel">
          <div>
            <div class="panel-label">产业热点</div>
            <h2>AI 算力链观察</h2>
            <p>聚焦 AI、半导体、PCB、光模块的景气度变化和基金持仓映射。</p>
          </div>
          <button class="panel-action" @click="goValuation">查看估值</button>
        </section>

        <section class="theme-grid">
          <article class="theme-card">
            <div class="theme-top">
              <span class="theme-tag">AI</span>
              <strong>算力需求</strong>
            </div>
            <p>训练和推理需求继续牵引服务器、GPU、存储与高速互联，重点关注订单兑现节奏。</p>
          </article>
          <article class="theme-card">
            <div class="theme-top">
              <span class="theme-tag">半导体</span>
              <strong>国产替代</strong>
            </div>
            <p>设备、材料、先进封装和存储周期共同影响板块弹性，估值修复更依赖业绩验证。</p>
          </article>
          <article class="theme-card">
            <div class="theme-top">
              <span class="theme-tag">PCB</span>
              <strong>高端板升级</strong>
            </div>
            <p>AI 服务器推动高多层板、HDI、载板需求提升，观察产能利用率和良率改善。</p>
          </article>
          <article class="theme-card">
            <div class="theme-top">
              <span class="theme-tag">光模块</span>
              <strong>高速互联</strong>
            </div>
            <p>800G/1.6T 升级是核心线索，关注云厂商资本开支与供应链交付能力。</p>
          </article>
        </section>

        <section class="insight-panel">
          <div class="section-heading">
            <h3>今日关注</h3>
            <span>{{ generatedAtText || '待更新' }}</span>
          </div>
          <div class="insight-list">
            <div class="insight-item">
              <span class="insight-dot hot"></span>
              <div>
                <strong>AI 主线</strong>
                <p>优先看业绩能落地的硬件环节，避免只按主题热度追高。</p>
              </div>
            </div>
            <div class="insight-item">
              <span class="insight-dot"></span>
              <div>
                <strong>半导体链</strong>
                <p>周期复苏和国产替代并行，短期波动通常来自库存与价格预期。</p>
              </div>
            </div>
            <div class="insight-item">
              <span class="insight-dot"></span>
              <div>
                <strong>PCB / 光模块</strong>
                <p>更适合结合基金重仓股涨跌看贡献，不只看单日涨幅。</p>
              </div>
            </div>
          </div>
        </section>
      </template>

      <template v-else-if="activeTab === 'valuation'">
        <div v-if="loading && funds.length === 0" class="state-page">
          <div class="spinner"></div>
          <div class="text">正在加载基金数据...</div>
        </div>

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
          <FundList
            v-if="showList"
            :funds="funds"
            :selected-code="selectedCode"
            @select="onFundSelect"
          />
          <FundDetail
            v-else-if="selectedDetail"
            :detail="selectedDetail"
            @holding-click="onHoldingClick"
          />
        </template>
      </template>

      <template v-else>
        <section class="profile-panel">
          <div class="profile-avatar">基</div>
          <h2>基金估值</h2>
          <p>当前为本地演示账户，数据来自估值接口。</p>
        </section>
        <div class="profile-list">
          <div>
            <span>接口状态</span>
            <strong>{{ error ? '异常' : '正常' }}</strong>
          </div>
          <div>
            <span>轮询频率</span>
            <strong>60 秒</strong>
          </div>
        </div>
      </template>
    </main>

    <nav class="bottom-nav" aria-label="主导航">
      <button :class="{ active: activeTab === 'valuation' }" @click="switchTab('valuation')">
        <span>▥</span>
        估值
      </button>
      <button :class="{ active: activeTab === 'news' }" @click="switchTab('news')">
        <span>⌂</span>
        资讯
      </button>
      <button :class="{ active: activeTab === 'profile' }" @click="switchTab('profile')">
        <span>◉</span>
        我的
      </button>
    </nav>

    <HoldingSheet
      v-if="selectedHolding"
      :holding="selectedHolding"
      :fund-name="currentFundName"
      @close="selectedHolding = null"
    />
  </div>
</template>

<script>
import FundList from './components/FundList.vue'
import FundDetail from './components/FundDetail.vue'
import HoldingSheet from './components/HoldingSheet.vue'
import { SNAPSHOT_API, readJsonResponse } from './utils/api.js'
import { normalizeSnapshot } from './utils/snapshot.js'

const POLL_INTERVAL = 60000

export default {
  components: { FundList, FundDetail, HoldingSheet },
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
      introTimer: null,
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
      if (!this.generatedAt) return ''
      const ts = this.generatedAt < 1000000000000 ? this.generatedAt * 1000 : this.generatedAt
      const d = new Date(ts)
      const pad = n => String(n).padStart(2, '0')
      return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    headerTitle() {
      if (this.activeTab === 'news') return '资讯'
      if (this.activeTab === 'profile') return '我的'
      return this.showList ? '基金估值' : (this.currentFundName || '基金详情')
    },
    risingFunds() {
      return this.funds.filter(f => Number(f.estimatedChangePct) >= 0).length
    },
    fallingFunds() {
      return this.funds.filter(f => Number(f.estimatedChangePct) < 0).length
    },
  },
  mounted() {
    this.loadData()
    this.startPolling()
    this.introTimer = setTimeout(() => {
      this.showIntro = false
    }, 1000)
  },
  unmounted() {
    this.stopPolling()
    if (this.introTimer) {
      clearTimeout(this.introTimer)
      this.introTimer = null
    }
  },
  methods: {
    switchTab(tab) {
      this.activeTab = tab
      if (tab !== 'valuation') {
        this.showList = true
      }
    },
    goValuation() {
      this.activeTab = 'valuation'
      this.showList = true
    },
    async loadData() {
      this.loading = this.funds.length === 0
      this.error = ''
      try {
        const res = await fetch(SNAPSHOT_API)
        const data = normalizeSnapshot(await readJsonResponse(res))
        const sf = data.funds
        const sd = data.details
        this.funds = sf
        this.details = sd
        this.generatedAt = data.generatedAt
        if (!this.selectedCode && sf.length > 0) {
          this.selectedCode = sf[0].fundCode
        }
      } catch (e) {
        this.error = e.message || '加载失败'
      } finally {
        this.loading = false
      }
    },
    refresh() {
      this.loadData()
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
  },
}
</script>
