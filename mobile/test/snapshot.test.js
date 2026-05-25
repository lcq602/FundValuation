import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeSnapshot } from '../src/utils/snapshot.js'

test('normalizeSnapshot converts API snake_case fields to component camelCase fields', () => {
  const normalized = normalizeSnapshot({
    generated_at: 1779682636,
    funds: [
      {
        fund_code: '012920',
        fund_name: '示例基金',
        estimated_nav: 4.1482,
        estimated_change_pct: 2.49,
        updated_at: '2026-04-30',
      },
    ],
    details: {
      '012920': {
        fund_code: '012920',
        fund_name: '示例基金',
        base_nav: 4.0475,
        estimated_nav: 4.1482,
        estimated_change_pct: 2.49,
        holdings: [
          {
            stock_code: 'TSM',
            stock_name: '台积电',
            change_pct: -0.65,
            last_price: 404.52,
            prev_close: 407.15,
            weight_contribution: -0.0008,
            ratio: 0.0707,
            status: 'ok',
          },
        ],
      },
    },
  })

  assert.equal(normalized.generatedAt, 1779682636)
  assert.deepEqual(normalized.funds[0], {
    fundCode: '012920',
    fundName: '示例基金',
    estimatedNav: 4.1482,
    estimatedChangePct: 2.49,
    baseNav: undefined,
    updatedAt: '2026-04-30',
    status: '',
    error: '',
    holdings: [],
  })
  assert.equal(normalized.details['012920'].baseNav, 4.0475)
  assert.equal(normalized.details['012920'].holdings[0].stockCode, 'TSM')
  assert.equal(normalized.details['012920'].holdings[0].weightContribution, -0.0008)
})

test('normalizeSnapshot tolerates missing collections', () => {
  assert.deepEqual(normalizeSnapshot({}), {
    funds: [],
    details: {},
    generatedAt: 0,
  })
})
