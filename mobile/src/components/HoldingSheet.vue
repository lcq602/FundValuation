<template>
  <div class="sheet-overlay" @click="$emit('close')">
    <div class="sheet" @click.stop>
      <div class="sheet-handle"></div>
      <div class="sheet-title">{{ fundName }}</div>
      <div class="sheet-stock-name">{{ holding.stockName }}</div>
      <div class="sheet-stock-code">{{ holding.stockCode }}</div>

      <div class="sheet-grid">
        <div class="sheet-card">
          <div class="val" :class="pctClass(holding.changePct)">
            {{ formatPct(holding.changePct) }}
          </div>
          <div class="lbl">涨跌幅</div>
        </div>
        <div class="sheet-card">
          <div class="val">{{ formatRatio(holding.ratio) }}</div>
          <div class="lbl">持仓比例</div>
        </div>
        <div class="sheet-card">
          <div class="val">{{ formatPrice(holding.lastPrice) }}</div>
          <div class="lbl">现价</div>
        </div>
        <div class="sheet-card">
          <div class="val">{{ formatPrice(holding.prevClose) }}</div>
          <div class="lbl">昨收</div>
        </div>
        <div class="sheet-card">
          <div class="val" :class="pctClass(holding.weightContribution)">
            {{ formatPct(holding.weightContribution) }}
          </div>
          <div class="lbl">对净值贡献</div>
        </div>
        <div class="sheet-card">
          <div class="val">{{ holding.status === 'ok' ? '正常' : holding.status }}</div>
          <div class="lbl">数据状态</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { formatPct, formatPrice, formatRatio, pctClass } from '../utils/formatters.js'

export default {
  props: {
    holding: { type: Object, required: true },
    fundName: { type: String, default: '' },
  },
  emits: ['close'],
  methods: {
    formatPct,
    formatPrice,
    formatRatio,
    pctClass,
  },
}
</script>
