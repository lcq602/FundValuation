import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeOverseasFunds, normalizeTimePeriod } from '../src/utils/overseas.js'

test('normalizeOverseasFunds converts snake_case overseas valuation fields', () => {
  const [fund] = normalizeOverseasFunds([
    {
      fund_code: '000041',
      fund_name: '华夏全球精选',
      estimated_change_pct: -0.68,
      us_contribution: -0.72,
      a_share_contribution: 0.04,
      time_period: 'US_MARKET_CLOSED',
      time_period_description: '美股已收盘',
      holdings: [
        {
          stock_code: 'NVDA',
          stock_name: '英伟达',
          change_pct: -1.2,
          last_price: 178.32,
          ratio: 0.0411,
          contribution: -0.05,
        },
      ],
    },
  ])

  assert.equal(fund.fundCode, '000041')
  assert.equal(fund.estimatedChangePct, -0.68)
  assert.equal(fund.usContribution, -0.72)
  assert.equal(fund.holdings[0].stockCode, 'NVDA')
})

test('normalizeTimePeriod converts snake_case time-period fields', () => {
  const period = normalizeTimePeriod({
    china_time: '09:04',
    us_time: '21:04',
    period_description: '美股已收盘',
  })

  assert.equal(period.chinaTime, '09:04')
  assert.equal(period.usTime, '21:04')
  assert.equal(period.periodDescription, '美股已收盘')
})
