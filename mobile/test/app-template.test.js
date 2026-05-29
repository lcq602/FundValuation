import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const appSource = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

test('App exposes imported formatter helpers used by Options API templates', () => {
  const methodsBlock = appSource.match(/methods:\s*\{([\s\S]*?)\n  \},\n\}/)?.[1] || ''

  assert.match(methodsBlock, /\bformatPct\b/)
  assert.match(methodsBlock, /\bpctClass\b/)
})
