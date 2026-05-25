<template>
  <div>
    <div style="font-size:13px;font-weight:600;margin:0 2px 6px;color:#666;">
      基金列表 ({{ funds.length }} 只)
    </div>
    <div
      v-for="f in funds"
      :key="f.fundCode"
      class="fund-card"
      :class="{ selected: f.fundCode === selectedCode }"
      @click="$emit('select', f.fundCode)"
    >
      <div>
        <div class="code">{{ f.fundCode }}</div>
        <div class="name">{{ f.fundName }}</div>
        <div class="nav">估值 {{ formatPrice(f.estimatedNav) }}</div>
      </div>
      <div class="change" :class="pctClass(f.estimatedChangePct)">
        {{ formatPct(f.estimatedChangePct) }}
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
  },
  emits: ['select'],
  methods: {
    formatPct,
    formatPrice,
    pctClass,
  },
}
</script>
