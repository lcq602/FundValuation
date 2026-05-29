<template>
  <div>
    <div class="list-header">
      <span>基金列表 ({{ funds.length }} 只)</span>
      <span v-if="selectedCount" class="selected-count">已选 {{ selectedCount }} 只</span>
    </div>

    <div v-if="funds.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M9 17H5a2 2 0 0 0-2 2 2 2 0 0 0 2 2h2a2 2 0 0 0 2-2zm12-2h-4a2 2 0 0 0-2 2 2 2 0 0 0 2 2h2a2 2 0 0 0 2-2z"></path>
        <path d="M5 17V7a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v10"></path>
      </svg>
      <p>没有找到匹配的基金</p>
    </div>

    <div v-else class="fund-list">
      <div
        v-for="f in funds"
        :key="f.fundCode"
        class="fund-card"
        :class="{ selected: f.fundCode === selectedCode }"
        @click="$emit('select', f.fundCode)"
      >
        <div class="card-main">
          <div class="fund-info">
            <div class="fund-code">{{ f.fundCode }}</div>
            <div class="fund-name">{{ f.fundName }}</div>
            <div class="fund-meta">
              <span>估值 {{ formatPrice(f.estimatedNav) }}</span>
              <span class="dot">·</span>
              <span>{{ f.updatedAt || '--' }}</span>
            </div>
          </div>
          <div class="change-section">
            <div class="change-pct" :class="pctClass(f.estimatedChangePct)">
              {{ formatPct(f.estimatedChangePct) }}
            </div>
            <div class="change-bar">
              <div
                class="change-fill"
                :class="pctClass(f.estimatedChangePct)"
                :style="changeBarStyle(f.estimatedChangePct)"
              ></div>
            </div>
          </div>
        </div>
        <button
          class="star-btn"
          :class="{ active: isFavorite(f.fundCode) }"
          @click.stop="$emit('toggle-favorite', f.fundCode)"
        >
          <svg viewBox="0 0 24 24" :fill="isFavorite(f.fundCode) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { formatPct, formatPrice, pctClass } from '../utils/formatters.js'

export default {
  props: {
    funds: { type: Array, default: () => [] },
    selectedCode: { type: String, default: '' },
    favorites: { type: Array, default: () => [] },
  },
  emits: ['select', 'toggle-favorite'],
  computed: {
    selectedCount() {
      return this.funds.filter(f => f.fundCode === this.selectedCode).length
    },
  },
  methods: {
    formatPct,
    formatPrice,
    pctClass,
    isFavorite(code) {
      return this.favorites.includes(code)
    },
    changeBarStyle(value) {
      const num = Math.min(Math.max(Number(value) || 0, -10), 10)
      const percent = Math.abs(num) * 10
      return { width: `${percent}%` }
    },
  },
}
</script>
