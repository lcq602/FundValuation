<template>
  <div class="detail-container">
    <!-- Contribution Chart -->
    <ContributionChart v-if="detail.holdings?.length > 0" :holdings="detail.holdings" />

    <!-- Status banners -->
    <div v-if="detail.status === 'partial'" class="banner warning">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <span>部分数据获取异常，估值可能不准确</span>
    </div>
    <div v-if="detail.error" class="banner error">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="15" y1="9" x2="9" y2="15"></line>
        <line x1="9" y1="9" x2="15" y2="15"></line>
      </svg>
      <span>错误: {{ detail.error }}</span>
    </div>

    <!-- Overview Card -->
    <section class="overview-card">
      <!-- Dynamic header gradient -->
      <div class="overview-header" :class="pctClass(detail.estimatedChangePct)">
        <div class="overview-info">
          <div class="fund-tag">基金净值估算</div>
          <h1 class="fund-name">{{ detail.fundName }}</h1>
          <p class="fund-code">{{ detail.fundCode }}</p>
        </div>
        <div class="change-panel">
          <div class="change-main" :class="pctClass(detail.estimatedChangePct)">
            <svg class="trend-icon" v-if="isPositive()" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
            <svg class="trend-icon trend-down" v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 18 13.5 8.5 8.5 13.5 1 6"></polyline>
              <polyline points="17 18 23 18 23 12"></polyline>
            </svg>
            <span class="change-value">{{ formatPct(detail.estimatedChangePct) }}</span>
          </div>
          <div class="change-bar-container">
            <div class="change-bar" :class="pctClass(detail.estimatedChangePct)"></div>
          </div>
        </div>
      </div>

      <div class="metrics-grid">
        <div class="metric-item">
          <div class="metric-value rise" v-if="isPositive()">
            {{ formatPrice(detail.estimatedNav) }}
          </div>
          <div class="metric-value fall" v-else>
            {{ formatPrice(detail.estimatedNav) }}
          </div>
          <div class="metric-label">估算净值</div>
        </div>
        <div class="metric-divider"></div>
        <div class="metric-item">
          <div class="metric-value">{{ formatPrice(detail.baseNav) }}</div>
          <div class="metric-label">前日净值</div>
        </div>
        <div class="metric-divider"></div>
        <div class="metric-item">
          <div class="metric-value">{{ detail.holdings?.length || 0 }}</div>
          <div class="metric-label">持仓数量</div>
        </div>
        <div class="metric-divider"></div>
        <div class="metric-item">
          <div class="metric-value">{{ detail.updatedAt || '--' }}</div>
          <div class="metric-label">净值日期</div>
        </div>
      </div>
    </section>

    <!-- Holdings Section -->
    <div class="section">
      <div class="section-header">
        <h2>持仓明细</h2>
        <span class="section-count">{{ detail.holdings?.length || 0 }} 只股票</span>
      </div>

      <div v-if="!detail.holdings || detail.holdings.length === 0" class="holdings-empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="18" height="18" rx="2"></rect>
        </svg>
        <p>暂无持仓数据</p>
      </div>

      <div
        v-for="h in detail.holdings"
        :key="h.stockCode"
        class="holding-item"
        @click="$emit('holding-click', h)"
      >
        <div class="holding-left">
          <div class="holding-name">{{ h.stockName }}</div>
          <div class="holding-code">{{ h.stockCode }}</div>
        </div>
        <div class="holding-right">
          <div class="holding-change" :class="pctClass(h.changePct)">
            {{ formatPct(h.changePct) }}
          </div>
          <div class="holding-stats">
            <span class="stat">{{ formatRatio(h.ratio) }} 比例</span>
            <span class="stat">{{ formatPrice(h.lastPrice) }} 现价</span>
            <span class="stat contribution" :class="pctClass(h.weightContribution)">
              {{ formatPct(h.weightContribution) }} 贡献
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Skeleton Loader (shown when loading) -->
    <SkeletonLoader v-if="loading" class="skeleton-panel" />
  </div>
</template>

<script setup>
import { pctClass, formatPct, formatPrice, formatRatio } from '../utils/formatters.js'
import ContributionChart from './ContributionChart.vue'
import SkeletonLoader from './SkeletonLoader.vue'

const props = defineProps({
  detail: { type: Object, required: true },
  isFavorite: { type: Function, required: true },
  loading: { type: Boolean, default: false },
})

defineEmits(['holding-click'])

const isPositive = () => {
  const num = Number(props.detail.estimatedChangePct) || 0
  return num > 0
}
</script>
