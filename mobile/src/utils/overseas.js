function numberValue(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : value
}

function normalizeHolding(holding = {}) {
  return {
    stockCode: holding.stockCode ?? holding.stock_code ?? '',
    stockName: holding.stockName ?? holding.stock_name ?? '',
    changePct: numberValue(holding.changePct ?? holding.change_pct),
    lastPrice: numberValue(holding.lastPrice ?? holding.last_price),
    ratio: numberValue(holding.ratio),
    contribution: numberValue(holding.contribution),
  }
}

export function normalizeOverseasFund(fund = {}) {
  return {
    fundCode: fund.fundCode ?? fund.fund_code ?? '',
    fundName: fund.fundName ?? fund.fund_name ?? '',
    estimatedChangePct: numberValue(fund.estimatedChangePct ?? fund.estimated_change_pct),
    usContribution: numberValue(fund.usContribution ?? fund.us_contribution),
    aShareContribution: numberValue(fund.aShareContribution ?? fund.a_share_contribution),
    timePeriod: fund.timePeriod ?? fund.time_period ?? '',
    timePeriodDescription: fund.timePeriodDescription ?? fund.time_period_description ?? '',
    holdings: Array.isArray(fund.holdings) ? fund.holdings.map(normalizeHolding) : [],
  }
}

export function normalizeOverseasFunds(funds = []) {
  return Array.isArray(funds) ? funds.map(normalizeOverseasFund) : []
}

export function normalizeTimePeriod(period = {}) {
  return {
    period: period.period ?? '',
    periodDescription: period.periodDescription ?? period.period_description ?? '',
    chinaTime: period.chinaTime ?? period.china_time ?? '',
    usTime: period.usTime ?? period.us_time ?? '',
  }
}
