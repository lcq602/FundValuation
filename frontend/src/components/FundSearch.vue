<template>
  <div>
    <div class="card">
      <div class="toolbar">
        <strong>查询线上基金持仓</strong>
      </div>
      <div class="form-group">
        <label>基金代码</label>
        <div style="display: flex; gap: 8px;">
          <input v-model="queryCode" placeholder="输入基金代码，如 000001" style="flex: 1;" @keyup.enter="search" />
          <button class="btn-primary" @click="search" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button v-if="result" class="btn-outline" @click="saveAsFund">保存为基金</button>
        </div>
      </div>
    </div>

    <div v-if="error" class="msg-bar error">{{ error }}</div>

    <template v-if="result">
      <div class="card">
        <div class="toolbar">
          <strong>{{ result.fund_code }} - {{ result.fund_name }}</strong>
          <span class="toolbar-count">持仓 {{ holdings.length }} 只</span>
          <span style="flex: 1;"></span>
          <button class="btn-ghost btn-sm" @click="copyJson">复制 JSON</button>
          <button class="btn-outline btn-sm" @click="copyMarkdown">复制 Markdown</button>
        </div>
        <table>
          <thead>
            <tr>
              <th style="width: 32px;">#</th>
              <th>股票代码</th>
              <th>股票名称</th>
              <th>持仓比例</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(h, i) in holdings" :key="i">
              <td>{{ i + 1 }}</td>
              <td>{{ h.stock_code }}</td>
              <td>{{ h.stock_name }}</td>
              <td>{{ h.ratio }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="holdings.length === 0" class="empty">暂无持仓</div>
      </div>

      <div class="card copy-wrapper">
        <strong style="font-size: 13px;">JSON 预览</strong>
        <button class="btn-ghost btn-sm copy-btn" @click="copyJson">复制</button>
        <textarea class="json-editor" readonly>{{ jsonText }}</textarea>
      </div>
    </template>
  </div>
</template>

<script>
export default {
  data() {
    return {
      queryCode: '',
      result: null,
      loading: false,
      error: '',
    }
  },
  computed: {
    holdings() {
      return this.result?.holdings || []
    },
    jsonText() {
      if (!this.result) return ''
      return JSON.stringify(this.result, null, 2)
    },
  },
  methods: {
    async search() {
      const code = this.queryCode.trim()
      if (!code) return
      this.loading = true
      this.error = ''
      this.result = null
      try {
        const res = await fetch(`/api/admin/funds/online/${code}`)
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))
          throw new Error(data.error || `HTTP ${res.status}`)
        }
        this.result = await res.json()
      } catch (e) {
        this.error = '查询失败: ' + e.message
      } finally {
        this.loading = false
      }
    },
    async saveAsFund() {
      if (!this.result) return
      try {
        const res = await fetch('/api/admin/funds', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.result),
        })
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))
          throw new Error(data.error || `HTTP ${res.status}`)
        }
        this.flashMsg('基金已保存!', 'success')
      } catch (e) {
        this.flashMsg('保存失败: ' + e.message, 'error')
      }
    },
    async copyJson() {
      try {
        await navigator.clipboard.writeText(this.jsonText)
        this.flashMsg('JSON 已复制', 'success')
      } catch {
        this.fallbackCopy(this.jsonText)
      }
    },
    async copyMarkdown() {
      if (!this.result) return
      let md = `| # | 股票代码 | 股票名称 | 持仓比例 |\n`
      md += `|---|---------|---------|---------|\n`
      this.holdings.forEach((h, i) => {
        md += `| ${i + 1} | ${h.stock_code} | ${h.stock_name} | ${h.ratio} |\n`
      })
      try {
        await navigator.clipboard.writeText(md)
        this.flashMsg('Markdown 已复制', 'success')
      } catch {
        this.fallbackCopy(md)
      }
    },
    fallbackCopy(text) {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      this.flashMsg('已复制', 'success')
    },
    flashMsg(msg, type) {
      const div = document.createElement('div')
      div.textContent = msg
      div.className = `toast toast-${type}`
      document.body.appendChild(div)
      setTimeout(() => div.remove(), 1500)
    },
  }
}
</script>