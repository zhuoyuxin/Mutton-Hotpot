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

    <!-- 待处理订单 -->
    <el-card style="margin-top:20px">
      <template #header>
        <span>待处理订单</span>
      </template>
      <div class="table-scroll">
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
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" type="primary" size="small" text @click="handleConfirm(row.id)">确认</el-button>
              <el-button v-if="getFirstPendingItem(row)" type="success" size="small" text @click="handleServe(getFirstPendingItem(row).id)">上菜</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!pendingLoading && pendingOrders.length === 0" description="暂无待处理订单" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getData } from '../../api/dashboard'
import { merchantList, confirm, serveItem } from '../../api/order'
import { orderStatusText, orderStatusType } from '../../utils/orderStatus'

const data = ref({})
const loading = ref(false)
const timer = ref(null)
const pendingOrders = ref([])
const pendingLoading = ref(false)

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
    /* ignore */
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
  const names = order.items.map(i => i.dishName + 'x' + i.quantity)
  return names.join('、')
}

const getFirstPendingItem = (order) => {
  if (!order.items) return null
  return order.items.find(i => i.status === 1) || null
}

const handleConfirm = async (id) => {
  try {
    await confirm(id)
    ElMessage.success('订单已确认')
    loadPendingOrders()
  } catch (e) {
    ElMessage.error('确认失败')
  }
}

const handleServe = async (itemId) => {
  try {
    await serveItem(itemId)
    ElMessage.success('已上菜')
    loadPendingOrders()
  } catch (e) {
    ElMessage.error('上菜操作失败')
  }
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
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  clearInterval(timer.value)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>
