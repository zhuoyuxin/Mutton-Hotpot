<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col v-for="table in tables" :key="table.id" :xs="12" :sm="8" :md="6" style="margin-bottom:16px">
        <el-card
          shadow="hover"
          :style="{ borderLeft: table.status === 1 ? '4px solid #e6a23c' : '4px solid #67c23a' }"
        >
          <div style="display:flex; justify-content:space-between; align-items:center">
            <span style="font-weight:bold">{{ table.name }}</span>
            <el-tag :type="table.status === 1 ? 'warning' : 'success'" size="small">
              {{ table.status === 1 ? '使用中' : '空闲' }}
            </el-tag>
          </div>
          <div v-if="table.area" style="color:#999; font-size:12px; margin-top:4px">{{ table.area }}</div>

          <template v-if="table.status === 1 && table.orders && table.orders.length > 0">
            <div style="margin-top:10px; font-size:13px">
              <span style="color:#67c23a">已上菜 {{ table.servedItems }}</span>
              <span style="margin:0 6px">/</span>
              <span style="color:#e6a23c">待上菜 {{ table.pendingItems }}</span>
              <span style="margin:0 6px">/</span>
              <span>共 {{ table.totalItems }} 项</span>
            </div>
            <div style="margin-top:6px; font-size:12px; color:#666; max-height:48px; overflow:hidden">
              {{ latestOrderSummary(table) }}
            </div>
          </template>

          <div style="margin-top:10px; display:flex; gap:6px">
            <el-button v-if="table.status === 1" size="small" type="warning" @click="goSessions(table.id)">结账</el-button>
            <el-button v-if="table.status === 1" size="small" @click="goOrders(table.id)">订单</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && tables.length === 0" description="暂无桌台" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { overview } from '../../api/table'

const router = useRouter()
const loading = ref(false)
const tables = ref([])
const timer = ref(null)

const loadTables = async () => {
  loading.value = true
  try {
    const res = await overview()
    tables.value = res.data
  } catch (e) {
    ElMessage.error('加载桌台数据失败')
  } finally {
    loading.value = false
  }
}

const latestOrderSummary = (table) => {
  if (!table.orders || table.orders.length === 0) return ''
  const latest = table.orders[0]
  if (!latest.items || latest.items.length === 0) return latest.orderNo
  return latest.items.slice(0, 3).map(i => i.dishName).join('、')
}

const goSessions = (tableId) => {
  router.push('/m/sessions')
}

const goOrders = (tableId) => {
  router.push({ path: '/m/orders', query: { tableId } })
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    clearInterval(timer.value)
    timer.value = null
  } else {
    loadTables()
    timer.value = setInterval(loadTables, 30000)
  }
}

onMounted(() => {
  loadTables()
  timer.value = setInterval(loadTables, 30000)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  clearInterval(timer.value)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>
