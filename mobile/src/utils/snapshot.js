function numberValue(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : value
}

export function normalizeHolding(holding = {}) {
  const weightContribution = numberValue(holding.weightContribution ?? holding.weight_contribution)

  return {
    stockCode: holding.stockCode ?? holding.stock_code ?? '',
    stockName: holding.stockName ?? holding.stock_name ?? '',
    marketSymbol: holding.marketSymbol ?? holding.market_symbol ?? '',
    ratio: numberValue(holding.ratio),
    changePct: numberValue(holding.changePct ?? holding.change_pct),
    lastPrice: numberValue(holding.lastPrice ?? holding.last_price),
    prevClose: numberValue(holding.prevClose ?? holding.prev_close),
    weightContribution,
    contribution: numberValue(holding.contribution ?? weightContribution),
    status: holding.status ?? '',
  }
}

export function normalizeFund(fund = {}) {
  return {
    fundCode: fund.fundCode ?? fund.fund_code ?? '',
    fundName: fund.fundName ?? fund.fund_name ?? '',
    estimatedNav: numberValue(fund.estimatedNav ?? fund.estimated_nav),
    estimatedChangePct: numberValue(fund.estimatedChangePct ?? fund.estimated_change_pct),
    baseNav: numberValue(fund.baseNav ?? fund.base_nav),
    updatedAt: fund.updatedAt ?? fund.updated_at ?? '',
    status: fund.status ?? '',
    error: fund.error ?? '',
    holdings: Array.isArray(fund.holdings) ? fund.holdings.map(normalizeHolding) : [],
  }
}

export function normalizeSnapshot(snapshot = {}) {
  const funds = Array.isArray(snapshot.funds) ? snapshot.funds.map(normalizeFund) : []
  const details = Object.fromEntries(
    Object.entries(snapshot.details || {}).map(([code, detail]) => [code, normalizeFund(detail)]),
  )

  return {
    funds,
    details,
    generatedAt: snapshot.generatedAt ?? snapshot.generated_at ?? 0,
  }
}
