<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px">
          <span>结账历史</span>
          <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期"
              value-format="YYYY-MM-DD" style="max-width:280px" />
            <el-button type="primary" @click="loadHistory">查询</el-button>
          </div>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="records" size="small">
          <el-table-column label="结账时间" width="160">
            <template #default="{ row }">{{ row.checkoutTime }}</template>
          </el-table-column>
          <el-table-column label="桌台" width="100">
            <template #default="{ row }">{{ row.tableName || '散客' }}</template>
          </el-table-column>
          <el-table-column label="总额(元)" width="100">
            <template #default="{ row }">{{ formatPrice(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="实收(元)" width="100">
            <template #default="{ row }">{{ formatPrice(row.actualPaid) }}</template>
          </el-table-column>
          <el-table-column label="折扣(元)" width="100">
            <template #default="{ row }">{{ formatPrice(row.discountAmount) }}</template>
          </el-table-column>
          <el-table-column label="积分" width="80">
            <template #default="{ row }">{{ row.pointsEarned }}</template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!loading && records.length === 0" description="暂无结账记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { history } from '../../api/session'
import { formatPrice } from '../../utils/format'

const loading = ref(false)
const records = ref([])
const dateRange = ref(null)

const loadHistory = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await history(params)
    records.value = res.data
  } catch (e) {
    ElMessage.error('加载结账历史失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.table-scroll {
  overflow-x: auto;
}
</style>
