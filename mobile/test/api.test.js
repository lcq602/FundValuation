import test from 'node:test'
import assert from 'node:assert/strict'

import * as api from '../src/utils/api.js'

const { readJsonResponse } = api

test('readJsonResponse parses valid JSON', async () => {
  const response = new Response('{"ok":true}', {
    status: 200,
    headers: { 'content-type': 'application/json' },
  })

  assert.deepEqual(await readJsonResponse(response), { ok: true })
})

test('readJsonResponse reports HTML responses as API address issues', async () => {
  const response = new Response('<!DOCTYPE html><html></html>', {
    status: 200,
    headers: { 'content-type': 'text/html' },
  })

  await assert.rejects(
    readJsonResponse(response),
    /接口返回了页面内容/,
  )
})

test('exports the backend time-period endpoint used by overseas valuation', () => {
  assert.equal(api.OVERSEAS_TIME_PERIOD_API, '/api/time-period')
})
