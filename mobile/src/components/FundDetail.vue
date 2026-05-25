<template>
  <div>
    <!-- Status banners -->
    <div v-if="detail.status === 'partial'" class="banner warning">
      部分数据获取异常，估值可能不准确
    </div>
    <div v-if="detail.error" class="banner error">
      错误: {{ detail.error }}
    </div>

    <!-- Overview card -->
    <div class="overview">
      <div class="overview-header">
        <div>
          <div class="overview-title">基金净值估算</div>
          <div class="overview-name">{{ detail.fundName }}</div>
          <div class="overview-code">{{ detail.fundCode }}</div>
        </div>
        <div class="overview-change">
          <div class="label">估算涨跌幅</div>
          <div class="value" :class="pctClass(detail.estimatedChangePct)">
            {{ formatPct(detail.estimatedChangePct) }}
          </div>
        </div>
      </div>

      <div class="overview-divider"></div>

      <div class="overview-metrics">
        <div class="metric">
          <div class="val" :class="pctClass(detail.estimatedChangePct)">
            {{ formatPrice(detail.estimatedNav) }}
          </div>
          <div class="lbl">估算净值</div>
        </div>
        <div class="metric">
          <div class="val">{{ formatPrice(detail.baseNav) }}</div>
          <div class="lbl">前日净值</div>
        </div>
        <div class="metric">
          <div class="val">{{ detail.holdings?.length || 0 }}</div>
          <div class="lbl">持仓数量</div>
        </div>
        <div class="metric">
          <div class="val">{{ detail.updatedAt || '--' }}</div>
          <div class="lbl">净值日期</div>
        </div>
      </div>
    </div>

    <!-- Holdings -->
    <div class="section-title">持仓明细</div>

    <div v-if="!detail.holdings || detail.holdings.length === 0" class="state-page" style="padding:30px 0;">
      <div style="font-size:32px;margin-bottom:6px;">&#x1F4ED;</div>
      <div class="text">暂无持仓数据</div>
    </div>

    <div
      v-for="h in detail.holdings"
      :key="h.stockCode"
      class="holding-card"
      @click="$emit('holding-click', h)"
    >
      <div class="holding-top">
        <div>
          <div class="holding-name">{{ h.stockName }}</div>
          <div class="holding-code">{{ h.stockCode }}</div>
        </div>
        <div class="holding-change" :class="pctClass(h.changePct)">
          {{ formatPct(h.changePct) }}
        </div>
      </div>
      <div class="holding-bottom">
        <span>比例 {{ formatRatio(h.ratio) }}</span>
        <span>现价 {{ formatPrice(h.lastPrice) }}</span>
        <span :class="pctClass(h.weightContribution)">
          贡献 {{ formatPct(h.weightContribution) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { formatPct, formatPrice, formatRatio, pctClass } from '../utils/formatters.js'

export default {
  props: {
    detail: { type: Object, required: true },
  },
  emits: ['holding-click'],
  methods: {
    formatPct,
    formatPrice,
    formatRatio,
    pctClass,
  },
}
</script>
