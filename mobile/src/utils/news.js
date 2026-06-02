function textValue(value) {
  return typeof value === 'string' ? value.trim() : ''
}

export function normalizeNewsItems(items = []) {
  if (!Array.isArray(items)) return []

  return items
    .map(item => ({
      title: textValue(item?.title),
      url: textValue(item?.url),
      source: textValue(item?.source),
      category: textValue(item?.category),
      publishedAt: textValue(item?.publishedAt ?? item?.published_at),
    }))
    .filter(item => item.title && item.url)
}
