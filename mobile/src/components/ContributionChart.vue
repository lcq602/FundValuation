<template>
  <section class="contribution-section">
    <div class="section-header">
      <h3>贡献分析</h3>
      <span class="section-badge" :class="totalClass">
        {{ totalLabel }}
      </span>
    </div>

    <div class="contribution-chart">
      <div
        v-for="(item, index) in sortedContributions"
        :key="item.stockCode"
        class="contribution-bar"
        :class="{ positive: item.weightContribution >= 0, negative: item.weightContribution < 0 }"
        :style="{ animationDelay: `${index * 50}ms` }"
      >
        <div class="bar-label">
          <span class="stock-name">{{ item.stockName }}</span>
          <span class="stock-ratio">{{ formatRatio(item.ratio) }}</span>
        </div>
        <div class="bar-track">
          <div
            class="bar-fill"
            :style="{ width: barWidth(item.weightContribution) }"
          ></div>
        </div>
        <div class="bar-value" :class="pctClass(item.weightContribution)">
          {{ formatPct(item.weightContribution) }}
        </div>
      </div>
    </div>

    <div class="contribution-summary">
      <div class="summary-item">
        <span class="summary-label">上涨贡献</span>
        <span class="summary-value rise">{{ formatPct(positiveSum) }}</span>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <span class="summary-label">下跌贡献</span>
        <span class="summary-value fall">{{ formatPct(negativeSum) }}</span>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <span class="summary-label">合计贡献</span>
        <span class="summary-value" :class="totalClass">{{ formatPct(totalContribution) }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { formatPct, formatRatio, pctClass } from '../utils/formatters.js'

const props = defineProps({
  holdings: { type: Array, default: () => [] },
})

const sortedContributions = computed(() => {
  if (!props.holdings?.length) return []
  return [...props.holdings]
    .filter(h => Number.isFinite(Number(h.weightContribution)) && Number(h.weightContribution) !== 0)
    .sort((a, b) => Math.abs(b.weightContribution) - Math.abs(a.weightContribution))
    .slice(0, 8)
})

const positiveSum = computed(() => {
  return sortedContributions.value
    .filter(h => h.weightContribution > 0)
    .reduce((sum, h) => sum + h.weightContribution, 0)
})

const negativeSum = computed(() => {
  return sortedContributions.value
    .filter(h => h.weightContribution < 0)
    .reduce((sum, h) => sum + h.weightContribution, 0)
})

const totalContribution = computed(() => {
  return positiveSum.value + negativeSum.value
})

const totalClass = computed(() => {
  return totalContribution.value >= 0 ? 'rise' : 'fall'
})

const totalLabel = computed(() => {
  const val = totalContribution.value
  if (val > 0) return `合计贡献 +${val.toFixed(2)}%`
  if (val < 0) return `合计贡献 ${val.toFixed(2)}%`
  return '贡献持平'
})

const barWidth = (value) => {
  const maxAbs = Math.max(
    ...sortedContributions.value.map(h => Math.abs(h.weightContribution)),
    0.01
  )
  const percent = Math.min(Math.abs(value) / maxAbs * 100, 100)
  return `${percent}%`
}
</script>
