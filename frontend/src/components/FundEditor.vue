<template>
  <div>
    <div v-if="!fundCode" class="empty">请从基金列表中选择一只基金进行编辑</div>

    <template v-else>
      <div class="toolbar">
        <h3 class="editor-title">{{ fundCode }} - {{ fundName }}</h3>
        <button class="btn-success" @click="save" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>

      <div v-if="saveMsg" :class="['msg-bar', saveMsg.type === 'ok' ? 'success' : 'error']">
        {{ saveMsg.text }}
      </div>

      <div class="card">
        <div class="form-group">
          <label>基金代码</label>
          <input v-model="fundCode" disabled />
        </div>
        <div class="form-group">
          <label>基金名称</label>
          <input v-model="fundName" />
        </div>
      </div>

      <div class="card">
        <div class="toolbar">
          <strong style="font-size: 14px;">持仓列表 ({{ holdings.length }} 只)</strong>
          <button class="btn-primary btn-sm" @click="addRow">+ 添加持仓</button>
        </div>
        <table class="holdings-table">
          <thead>
            <tr>
              <th style="width: 32px;">#</th>
              <th>股票代码</th>
              <th>股票名称</th>
              <th style="width: 110px;">持仓比例</th>
              <th style="width: 48px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(h, i) in holdings" :key="i">
              <td>{{ i + 1 }}</td>
              <td><input v-model="h.stock_code" placeholder="如 00700" /></td>
              <td><input v-model="h.stock_name" placeholder="股票名称" /></td>
              <td><input v-model="h.ratio" placeholder="如 0.15" type="number" step="0.001" /></td>
              <td>
                <button class="btn-xs" style="background:var(--danger-light);color:var(--danger);" @click="removeRow(i)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="holdings.length === 0" class="empty">暂无持仓数据</div>
      </div>
    </template>
  </div>
</template>

<script>
export default {
  props: {
    fundData: { type: Object, default: null },
  },
  emits: ['saved'],
  data() {
    return {
      fundCode: '',
      fundName: '',
      holdings: [],
      saving: false,
      saveMsg: null,
    }
  },
  watch: {
    fundData: {
      immediate: true,
      handler(val) {
        if (val) {
          this.loadFund(val.code)
        }
      }
    }
  },
  methods: {
    async loadFund(code) {
      this.saveMsg = null
      try {
        const res = await fetch(`/api/admin/funds/${code}`)
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const data = await res.json()
        this.fundCode = data.fund_code
        this.fundName = data.fund_name || ''
        this.holdings = (data.holdings || []).map(h => ({
          stock_code: h.stock_code || '',
          stock_name: h.stock_name || '',
          ratio: h.ratio ?? '',
        }))
      } catch (e) {
        this.saveMsg = { type: 'err', text: '加载失败: ' + e.message }
      }
    },
    addRow() {
      this.holdings.push({ stock_code: '', stock_name: '', ratio: '' })
    },
    removeRow(i) {
      this.holdings.splice(i, 1)
    },
    async save() {
      this.saving = true
      this.saveMsg = null
      try {
        const body = JSON.stringify({
          fund_code: this.fundCode,
          fund_name: this.fundName,
          holdings: this.holdings.map(h => ({
            stock_code: h.stock_code,
            stock_name: h.stock_name,
            ratio: h.ratio === '' ? 0 : parseFloat(h.ratio),
          })),
        })
        const res = await fetch(`/api/admin/funds/${this.fundCode}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body,
        })
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        this.saveMsg = { type: 'ok', text: '保存成功!' }
        this.$emit('saved')
        setTimeout(() => { this.saveMsg = null }, 2000)
      } catch (e) {
        this.saveMsg = { type: 'err', text: '保存失败: ' + e.message }
      } finally {
        this.saving = false
      }
    },
  }
}
</script>