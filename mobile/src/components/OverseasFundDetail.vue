<template>
  <div class="sheet-overlay" @click="$emit('close')">
    <div class="overseas-sheet" @click.stop>
      <div class="sheet-handle"></div>

      <!-- Header -->
      <div class="sheet-header">
        <div class="fund-basic">
          <span class="fund-tag">QDII</span>
          <h2 class="fund-name">{{ fund.fundName }}</h2>
          <p class="fund-code">{{ fund.fundCode }}</p>
        </div>
        <div class="fund-summary">
          <div class="summary-change" :class="pctClass(fund.estimatedChangePct)">
            {{ formatPct(fund.estimatedChangePct) }}
          </div>
          <div class="summary-label">估算涨跌</div>
        </div>
      </div>

      <!-- Time Period Info -->
      <div class="period-info">
        <span class="period-badge" :class="fund.timePeriod === 'US_MARKET_OPEN' ? 'open' : 'closed'">
          {{ fund.timePeriod === 'US_MARKET_OPEN' ? '美股盘中' : '美股已收盘' }}
        </span>
        <span class="period-desc">{{ fund.timePeriodDescription }}</span>
      </div>

      <!-- Contribution Summary -->
      <div class="contribution-summary">
        <div class="contrib-card rise">
          <div class="contrib-value">{{ formatPct(fund.usContribution) }}</div>
          <div class="contrib-label">美股贡献</div>
        </div>
        <div class="contrib-card neutral">
          <div class="contrib-value">{{ formatPct(fund.aShareContribution) }}</div>
          <div class="contrib-label">A股贡献</div>
        </div>
        <div class="contrib-card" :class="pctClass(fund.estimatedChangePct)">
          <div class="contrib-value">{{ formatPct(fund.estimatedChangePct) }}</div>
          <div class="contrib-label">合计贡献</div>
        </div>
      </div>

      <!-- Holdings -->
      <div class="holdings-section">
        <h3 class="section-title">持仓明细</h3>
        <div class="holdings-list">
          <div
            v-for="h in fund.holdings"
            :key="h.stockCode"
            class="holding-item"
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
                <span class="stat">{{ formatRatio(h.ratio) }}</span>
                <span class="stat">{{ formatPrice(h.lastPrice) }}</span>
                <span class="stat contribution" :class="pctClass(h.contribution)">
                  贡献 {{ formatPct(h.contribution) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Close Button -->
      <button class="close-btn" @click="$emit('close')">关闭</button>
    </div>
  </div>
</template>

<script setup>
import { formatPct, formatPrice, formatRatio, pctClass } from '../utils/formatters.js'

defineProps({
  fund: { type: Object, required: true },
})

defineEmits(['close'])
</script>

<style scoped>
.overseas-sheet {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--card-bg);
  border-radius: 20px 20px 0 0;
  padding: 20px 20px calc(80px + var(--safe-bottom));
  z-index: 201;
  animation: slideUp 0.3s ease-out;
  max-height: 85vh;
  overflow-y: auto;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-handle {
  width: 32px;
  height: 4px;
  background: #ddd;
  border-radius: 2px;
  margin: -8px auto 16px;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.fund-basic .fund-tag {
  display: inline-block;
  padding: 2px 8px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 4px;
}

.fund-basic .fund-name {
  font-size: 19px;
  font-weight: 700;
  margin: 0;
  line-height: 1.35;
}

.fund-basic .fund-code {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 2px 0 0;
}

.fund-summary {
  text-align: right;
}

.summary-change {
  font-size: 24px;
  font-weight: 700;
}

.summary-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.period-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--bg);
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid var(--border);
}

.period-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.period-badge.open {
  background: rgba(39, 174, 96, 0.15);
  color: var(--fall);
}

.period-badge.closed {
  background: rgba(140, 140, 160, 0.15);
  color: var(--text-secondary);
}

.period-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.contribution-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 20px;
}

.contrib-card {
  background: var(--bg);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  border: 1px solid var(--border);
}

.contrib-value {
  font-size: 16px;
  font-weight: 700;
}

.contrib-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.contrib-card.rise .contrib-value {
  color: var(--rise);
}

.contrib-card.fall .contrib-value {
  color: var(--fall);
}

.contrib-card.neutral .contrib-value {
  color: var(--text-secondary);
}

.holdings-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 12px;
}

.holdings-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.holding-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--bg);
  border-radius: 8px;
  border: 1px solid var(--border);
}

.holding-left {
  flex: 1;
  min-width: 0;
}

.holding-name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.holding-code {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.holding-right {
  text-align: right;
  min-width: 100px;
}

.holding-change {
  font-size: 16px;
  font-weight: 700;
}

.holding-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: flex-end;
  margin-top: 4px;
}

.holding-stats .stat {
  font-size: 11px;
  color: var(--text-secondary);
}

.holding-stats .contribution {
  font-weight: 600;
}

.close-btn {
  width: 100%;
  padding: 12px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}

.close-btn:active {
  opacity: 0.8;
}

/* Dark mode */
.dark .sheet-handle {
  background: #4a4a5a;
}

.dark .overseas-sheet {
  background: var(--card-bg);
}
</style>
