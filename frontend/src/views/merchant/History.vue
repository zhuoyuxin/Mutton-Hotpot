<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div>
            <div class="page-title">结账历史</div>
            <div class="page-subtitle">按日期查看每次结账的菜品、自助费、餐具费和实收明细。</div>
          </div>
          <div class="filters">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="max-width: 280px"
            />
            <el-button type="primary" @click="loadHistory">查询</el-button>
          </div>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="records" size="small">
          <el-table-column label="结账时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.checkoutTime) }}</template>
          </el-table-column>
          <el-table-column label="桌台" min-width="130">
            <template #default="{ row }">{{ formatTableLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="菜品金额(元)" width="120">
            <template #default="{ row }">{{ formatPrice(row.dishAmount) }}</template>
          </el-table-column>
          <el-table-column label="自助费" min-width="150">
            <template #default="{ row }">{{ formatHeadcountFee(row.selfServiceCount, row.selfServiceUnitPrice, row.selfServiceAmount) }}</template>
          </el-table-column>
          <el-table-column label="餐具费" min-width="150">
            <template #default="{ row }">{{ formatHeadcountFee(row.tablewareCount, row.tablewareUnitPrice, row.tablewareAmount) }}</template>
          </el-table-column>
          <el-table-column label="应收(元)" width="110">
            <template #default="{ row }">{{ formatPrice(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="实收(元)" width="110">
            <template #default="{ row }">{{ formatPrice(row.actualPaid) }}</template>
          </el-table-column>
          <el-table-column label="优惠(元)" width="110">
            <template #default="{ row }">{{ formatPrice(row.discountAmount) }}</template>
          </el-table-column>
          <el-table-column label="积分" width="90">
            <template #default="{ row }">{{ row.pointsEarned || 0 }}</template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-if="!loading && records.length === 0" description="暂无结账记录" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { history } from '../../api/session'
import { formatPrice } from '../../utils/format'

const loading = ref(false)
const records = ref([])
const dateRange = ref(null)

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').split('.')[0]
}

const formatTableLabel = (row) => {
  const tableName = row.tableName || '散台'
  return row.tableArea ? `${row.tableArea} / ${tableName}` : tableName
}

const formatHeadcountFee = (count, unitPrice, amount) => {
  const normalizedAmount = Number(amount || 0)
  const normalizedCount = Number(count || 0)
  const normalizedUnitPrice = Number(unitPrice || 0)
  if (normalizedAmount <= 0 && normalizedCount <= 0) {
    return '-'
  }
  return `${formatPrice(normalizedAmount)} 元 / ${normalizedCount} 人 / ${formatPrice(normalizedUnitPrice)} 元`
}

const loadHistory = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await history(params)
    records.value = res.data || []
  } catch (error) {
    ElMessage.error('加载结账历史失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #2f241f;
}

.page-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #8a7468;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.table-scroll {
  overflow-x: auto;
}
</style>
