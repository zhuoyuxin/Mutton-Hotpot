<template>
  <div v-loading="loading">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="今日订单" :value="data.todayOrders || 0" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="今日营收(元)" :value="(data.todayRevenue || 0) / 100" :precision="2" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="空闲桌台" :value="data.freeTables || 0" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover">
          <el-statistic title="进行中会话" :value="data.activeSessions || 0" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header>
        <span>待处理订单</span>
      </template>

      <div v-if="isMobile" class="pending-mobile-list" v-loading="pendingLoading">
        <div v-for="row in pendingOrders" :key="row.id" class="pending-mobile-card">
          <div class="pending-card-head">
            <div class="pending-card-title">{{ row.orderNo }}</div>
            <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </div>
          <div class="pending-card-meta">{{ row.tableName || '散客' }} / {{ row.createTime }}</div>
          <div class="pending-card-summary">{{ dishSummary(row) }}</div>
          <div class="pending-card-actions">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click="handleConfirm(row.id)"
            >
              确认
            </el-button>
            <el-button
              v-if="hasPendingServeItems(row)"
              type="success"
              size="small"
              @click="openServeDialog(row)"
            >
              上菜
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="table-scroll">
        <el-table :data="pendingOrders" size="small" v-loading="pendingLoading">
          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column label="桌台" width="100">
            <template #default="{ row }">{{ row.tableName || '散客' }}</template>
          </el-table-column>
          <el-table-column label="菜品摘要">
            <template #default="{ row }">
              {{ dishSummary(row) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ row.createTime }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                type="primary"
                size="small"
                text
                @click="handleConfirm(row.id)"
              >
                确认
              </el-button>
              <el-button
                v-if="hasPendingServeItems(row)"
                type="success"
                size="small"
                text
                @click="openServeDialog(row)"
              >
                上菜
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-if="!pendingLoading && pendingOrders.length === 0" description="暂无待处理订单" :image-size="60" />
    </el-card>

    <ServeItemsDialog
      v-model:visible="showServeDialog"
      :order="servingOrder"
      @served="refreshAll"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getData } from '../../api/dashboard'
import { merchantList, confirm } from '../../api/order'
import { orderStatusText, orderStatusType } from '../../utils/orderStatus'
import ServeItemsDialog from '../../components/ServeItemsDialog.vue'

const data = ref({})
const loading = ref(false)
const timer = ref(null)
const pendingOrders = ref([])
const pendingLoading = ref(false)
const showServeDialog = ref(false)
const servingOrder = ref(null)
const isMobile = ref(window.innerWidth <= 768)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getData()
    data.value = res.data
  } catch (e) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const loadPendingOrders = async () => {
  pendingLoading.value = true
  try {
    const res = await merchantList({ statuses: '0,1,2' })
    pendingOrders.value = res.data
  } catch (e) {
    ElMessage.error('加载待处理订单失败')
  } finally {
    pendingLoading.value = false
  }
}

const refreshAll = () => {
  loadData()
  loadPendingOrders()
}

const dishSummary = (order) => {
  if (!order.items || order.items.length === 0) return '-'
  const summaryMap = new Map()
  order.items.forEach((item) => {
    summaryMap.set(item.dishName, (summaryMap.get(item.dishName) || 0) + item.quantity)
  })
  return Array.from(summaryMap.entries())
    .map(([name, quantity]) => `${name}x${quantity}`)
    .join('、')
}

const hasPendingServeItems = (order) => {
  if (!order.items) return false
  return order.items.some((item) => item.status === 1)
}

const handleConfirm = async (id) => {
  try {
    await confirm(id)
    ElMessage.success('订单已确认')
    refreshAll()
  } catch (e) {
    ElMessage.error('确认失败')
  }
}

const openServeDialog = (order) => {
  servingOrder.value = order
  showServeDialog.value = true
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    clearInterval(timer.value)
    timer.value = null
  } else {
    refreshAll()
    timer.value = setInterval(refreshAll, 30000)
  }
}

onMounted(() => {
  refreshAll()
  timer.value = setInterval(refreshAll, 30000)
  window.addEventListener('resize', handleResize)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  clearInterval(timer.value)
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.pending-mobile-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pending-mobile-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.pending-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.pending-card-title {
  font-weight: 600;
  word-break: break-all;
}

.pending-card-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.pending-card-summary {
  margin-top: 10px;
  color: #303133;
  line-height: 1.5;
}

.pending-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.table-scroll {
  overflow-x: auto;
}
</style>
