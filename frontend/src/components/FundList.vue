<template>
  <div>
    <div class="toolbar">
      <input
        v-model="searchText"
        placeholder="搜索基金代码或名称..."
        style="width: 200px;"
      />
      <button class="btn-primary" @click="showCreate = true">+ 新建基金</button>
      <span style="margin-left: auto; font-size: 12px; color: #999;">共 {{ filteredList.length }} 只基金</span>
    </div>

    <div class="card">
      <div
        v-for="item in filteredList"
        :key="item.code"
        class="fund-list-item"
        :class="{ selected: selectedCode === item.code }"
        @click="select(item)"
      >
        <div>
          <span class="code">{{ item.code }}</span>
          <span class="name">{{ item.name }}</span>
        </div>
        <div class="actions">
          <button class="btn-outline btn-sm" @click.stop="select(item)">编辑</button>
          <button class="btn-danger btn-sm" @click.stop="confirmDelete(item)">删除</button>
        </div>
      </div>
      <div v-if="filteredList.length === 0" class="empty">暂无基金数据</div>
    </div>

    <!-- 新建弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>新建基金</h3>
        <div class="form-group">
          <label>基金代码</label>
          <input v-model="newFund.code" placeholder="例如 000001" />
        </div>
        <div class="form-group">
          <label>基金名称</label>
          <input v-model="newFund.name" placeholder="例如 示例基金" />
        </div>
        <div class="modal-actions">
          <button class="btn-outline" @click="showCreate = false">取消</button>
          <button class="btn-primary" @click="createFund">创建</button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal">
        <h3>确认删除</h3>
        <p>确定要删除基金 <strong>{{ deleteTarget.code }} - {{ deleteTarget.name }}</strong> 吗？</p>
        <div class="modal-actions">
          <button class="btn-outline" @click="deleteTarget = null">取消</button>
          <button class="btn-danger" @click="doDelete">删除</button>
        </div>
      </div>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script>
export default {
  emits: ['edit-fund'],
  data() {
    return {
      fundList: [],
      searchText: '',
      selectedCode: null,
      showCreate: false,
      deleteTarget: null,
      newFund: { code: '', name: '' },
      error: '',
    }
  },
  computed: {
    filteredList() {
      const q = this.searchText.trim().toLowerCase()
      if (!q) return this.fundList
      return this.fundList.filter(f =>
        f.code.toLowerCase().includes(q) || f.name.toLowerCase().includes(q)
      )
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      try {
        const res = await fetch('/api/admin/funds')
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        this.fundList = await res.json()
      } catch (e) {
        this.error = '加载基金列表失败: ' + e.message
      }
    },
    select(item) {
      this.selectedCode = item.code
      this.$emit('edit-fund', item)
    },
    async createFund() {
      if (!this.newFund.code || !this.newFund.name) {
        this.error = '请填写基金代码和名称'
        return
      }
      try {
        const res = await fetch('/api/admin/funds', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            fund_code: this.newFund.code,
            fund_name: this.newFund.name,
            holdings: [],
          }),
        })
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        this.showCreate = false
        this.newFund = { code: '', name: '' }
        this.error = ''
        await this.fetchList()
      } catch (e) {
        this.error = '创建失败: ' + e.message
      }
    },
    confirmDelete(item) {
      this.deleteTarget = item
    },
    async doDelete() {
      try {
        const res = await fetch(`/api/admin/funds/${this.deleteTarget.code}`, {
          method: 'DELETE',
        })
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        this.deleteTarget = null
        this.error = ''
        await this.fetchList()
      } catch (e) {
        this.error = '删除失败: ' + e.message
      }
    },
  }
}
</script>