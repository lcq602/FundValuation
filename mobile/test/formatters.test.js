import test from 'node:test'
import assert from 'node:assert/strict'

import { formatPct, formatPrice, formatRatio, pctClass } from '../src/utils/formatters.js'

test('formatPct handles missing and invalid values without throwing', () => {
  assert.equal(formatPct(undefined), '--')
  assert.equal(formatPct(null), '--')
  assert.equal(formatPct(''), '--')
  assert.equal(formatPct('not-a-number'), '--')
})

test('formatPct formats numeric values with signs', () => {
  assert.equal(formatPct(1.234), '+1.23%')
  assert.equal(formatPct(-0.456), '-0.46%')
  assert.equal(formatPct('0'), '+0.00%')
})

test('formatPrice keeps zero and hides missing values', () => {
  assert.equal(formatPrice(0), '0.0000')
  assert.equal(formatPrice('1.23456'), '1.2346')
  assert.equal(formatPrice(undefined), '--')
})

test('formatRatio formats decimal ratios as percentages', () => {
  assert.equal(formatRatio(0.1234), '12.34%')
  assert.equal(formatRatio(undefined), '--')
})

test('pctClass only returns rise or fall for valid numbers', () => {
  assert.equal(pctClass(0), 'rise')
  assert.equal(pctClass(-0.1), 'fall')
  assert.equal(pctClass(undefined), '')
})
