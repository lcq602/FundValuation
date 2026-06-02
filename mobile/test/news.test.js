import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeNewsItems } from '../src/utils/news.js'

test('normalizeNewsItems preserves metadata for in-app news detail', () => {
  const [item] = normalizeNewsItems([
    {
      title: 'AI 算力产业链持续活跃',
      url: 'https://finance.eastmoney.com/a/202606013412345678.html',
      source: '东方财富',
      category: '财经',
      published_at: '2026-06-01 10:30',
    },
  ])

  assert.equal(item.title, 'AI 算力产业链持续活跃')
  assert.equal(item.url, 'https://finance.eastmoney.com/a/202606013412345678.html')
  assert.equal(item.source, '东方财富')
  assert.equal(item.category, '财经')
  assert.equal(item.publishedAt, '2026-06-01 10:30')
})

test('normalizeNewsItems ignores invalid records', () => {
  const items = normalizeNewsItems([
    { title: '缺少链接' },
    { url: 'https://example.com/empty-title.html' },
    null,
    {
      title: '有效资讯',
      url: 'https://finance.eastmoney.com/a/202606013412345678.html',
    },
  ])

  assert.equal(items.length, 1)
  assert.equal(items[0].title, '有效资讯')
})
