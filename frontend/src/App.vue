<template>
  <div>
    <header class="header">
      <h1>FundValuation</h1>
      <nav>
        <a href="#" :class="{ active: tab === 'funds' }" @click.prevent="tab = 'funds'">基金列表</a>
        <a href="#" :class="{ active: tab === 'editor' }" @click.prevent="tab = 'editor'">编辑器</a>
        <a href="#" :class="{ active: tab === 'search' }" @click.prevent="tab = 'search'">查询</a>
      </nav>
    </header>
    <div class="container">
      <FundList
        v-show="tab === 'funds'"
        @edit-fund="editFund"
      />
      <FundEditor
        v-show="tab === 'editor'"
        :fund-data="editingFund"
        @saved="onSaved"
      />
      <FundSearch
        v-show="tab === 'search'"
      />
    </div>
  </div>
</template>

<script>
import FundList from './components/FundList.vue'
import FundEditor from './components/FundEditor.vue'
import FundSearch from './components/FundSearch.vue'

export default {
  components: { FundList, FundEditor, FundSearch },
  data() {
    return {
      tab: 'funds',
      editingFund: null,
    }
  },
  methods: {
    editFund(fund) {
      this.editingFund = fund
      this.tab = 'editor'
    },
    onSaved() {
      this.editingFund = null
    }
  }
}
</script>