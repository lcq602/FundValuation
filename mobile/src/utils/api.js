const isProd = import.meta.env?.PROD === true
const API_BASE = isProd ? 'http://140.143.235.93:5000' : ''

export const SNAPSHOT_API = `${API_BASE}/api/snapshot`

export async function readJsonResponse(response) {
  const contentType = response.headers.get('content-type') || ''
  const body = await response.text()

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  if (!contentType.includes('application/json') && body.trimStart().startsWith('<')) {
    throw new Error('接口返回了页面内容，请检查 API 地址')
  }

  try {
    return JSON.parse(body)
  } catch {
    throw new Error('接口返回的数据不是有效 JSON')
  }
}
