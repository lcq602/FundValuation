function toFiniteNumber(value) {
  if (value === undefined || value === null || value === '') return null

  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

export function formatPct(value) {
  const number = toFiniteNumber(value)
  if (number === null) return '--'

  return `${number >= 0 ? '+' : ''}${number.toFixed(2)}%`
}

export function formatPrice(value) {
  const number = toFiniteNumber(value)
  if (number === null) return '--'

  return number.toFixed(4)
}

export function formatRatio(value) {
  const number = toFiniteNumber(value)
  if (number === null) return '--'

  return `${(number * 100).toFixed(2)}%`
}

export function pctClass(value) {
  const number = toFiniteNumber(value)
  if (number === null) return ''

  return number >= 0 ? 'rise' : 'fall'
}
