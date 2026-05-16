<template>
  <div v-loading="loading">
    <el-card style="margin-bottom: 16px">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap">
          <span>桌台运营</span>
          <span style="color:#909399; font-size:12px">查看桌台状态、订单进度，并直接完成结账</span>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col
          v-for="table in tables"
          :key="table.id"
          :xs="12"
          :sm="8"
          :md="6"
          style="margin-bottom:16px"
        >
          <el-card
            shadow="hover"
            :style="{ borderLeft: table.status === 1 ? '4px solid #e6a23c' : '4px solid #67c23a' }"
          >
            <div style="display:flex; justify-content:space-between; align-items:center; gap:8px">
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
                <span style="color:#e6a23c">待处理 {{ table.pendingItems }}</span>
                <span style="margin:0 6px">/</span>
                <span>共 {{ table.totalItems }} 项</span>
              </div>
              <div style="margin-top:6px; font-size:12px; color:#666; min-height:34px">
                {{ latestOrderSummary(table) || '暂无菜品摘要' }}
              </div>
            </template>

            <template v-else>
              <div style="margin-top:10px; font-size:12px; color:#909399; min-height:34px">
                当前桌台空闲，可等待顾客扫码点餐。
              </div>
            </template>

            <div style="margin-top:10px; display:flex; gap:6px; flex-wrap:wrap">
              <el-button
                v-if="table.status === 1"
                size="small"
                type="primary"
                @click="openSession(table)"
              >
                会话详情
              </el-button>
              <el-button
                v-if="table.status === 1"
                size="small"
                type="warning"
                @click="openSession(table, true)"
              >
                结账
              </el-button>
              <el-button
                v-if="table.status === 1"
                size="small"
                @click="goOrders(table.id)"
              >
                订单
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && tables.length === 0" description="暂无桌台" />
    </el-card>

    <el-dialog
      v-model="showSessionDialog"
      class="session-dialog"
      :fullscreen="isMobile"
      title="会话详情与结账"
      :width="isMobile ? '92vw' : '760px'"
      @closed="resetSessionDialog"
    >
      <div v-if="sessionDetail" v-loading="detailLoading" class="session-dialog-body">
        <div style="display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; margin-bottom:12px">
          <div>
            <div style="font-weight:600">{{ currentTable?.name }}<span v-if="currentTable?.area" style="color:#909399; font-weight:400"> / {{ currentTable.area }}</span></div>
            <div style="font-size:12px; color:#909399; margin-top:4px">会话 ID：{{ sessionDetail.id }}</div>
          </div>
          <div style="font-size:16px">
            应结总额：
            <span style="color:#f56c6c; font-weight:600">{{ (sessionDetail.totalAmount / 100).toFixed(2) }} 元</span>
          </div>
        </div>

        <el-collapse v-model="expandedOrders">
          <el-collapse-item
            v-for="order in sessionDetail.orders"
            :key="order.id"
            :name="order.id"
          >
            <template #title>
              <div class="session-order-title">
                <span>{{ order.orderNo }}</span>
                <span style="color:#909399">{{ order.createTime }}</span>
              </div>
            </template>

            <div class="table-scroll">
              <el-table :data="order.items || []" size="small">
                <el-table-column prop="dishName" label="菜品" />
                <el-table-column prop="quantity" label="数量" width="70" />
                <el-table-column label="单价(元)" width="90">
                  <template #default="{ row }">{{ (row.dishPrice / 100).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" :type="itemStatusType(row.status)">{{ itemStatusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-collapse-item>
        </el-collapse>

        <el-divider />

        <el-form
          :model="checkoutForm"
          :label-width="isMobile ? 'auto' : '110px'"
          :label-position="isMobile ? 'top' : 'right'"
          class="checkout-form"
        >
          <el-form-item label="实收金额(元)">
            <el-input-number
              v-model="checkoutForm.actualPaid"
              :min="0"
              :precision="2"
              :step="1"
              style="width:100%"
            />
          </el-form-item>
          <el-form-item label="手机号(可选)">
            <el-input v-model="checkoutForm.phone" placeholder="填写手机号可累计积分" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showSessionDialog = false">关闭</el-button>
        <el-button type="primary" :loading="checkoutLoading" @click="handleCheckout">确认结账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { overview } from '../../api/table'
import { current, detail, checkout } from '../../api/session'
import { itemStatusText, itemStatusType } from '../../utils/orderStatus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const detailLoading = ref(false)
const checkoutLoading = ref(false)
const tables = ref([])
const timer = ref(null)
const isMobile = ref(window.innerWidth <= 768)

const showSessionDialog = ref(false)
const currentTable = ref(null)
const sessionDetail = ref(null)
const checkoutForm = ref({ actualPaid: 0, phone: '' })
const expandedOrders = ref([])
const pendingOpenTableId = ref(null)
const openCheckoutDirectly = ref(false)

const loadTables = async () => {
  loading.value = true
  try {
    const res = await overview()
    tables.value = res.data
    await tryOpenPendingTable()
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
  return latest.items.slice(0, 3).map((item) => item.dishName).join('、')
}

const goOrders = (tableId) => {
  router.push({ path: '/m/orders', query: { tableId } })
}

const openSession = async (table, checkoutDirectly = false) => {
  currentTable.value = table
  detailLoading.value = true
  try {
    const sessionRes = await current(table.id)
    if (!sessionRes.data) {
      ElMessage.warning('该桌台当前没有进行中的会话')
      return
    }

    const detailRes = await detail(sessionRes.data.id)
    const data = detailRes.data
    sessionDetail.value = {
      ...data.session,
      orders: data.orders || [],
      totalAmount: data.totalAmount || 0
    }
    expandedOrders.value = checkoutDirectly
      ? []
      : sessionDetail.value.orders.map((order) => order.id)
    checkoutForm.value = {
      actualPaid: sessionDetail.value.totalAmount / 100,
      phone: ''
    }
    showSessionDialog.value = true
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载会话详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleCheckout = async () => {
  if (!sessionDetail.value) {
    return
  }

  checkoutLoading.value = true
  try {
    await checkout(sessionDetail.value.id, checkoutForm.value)
    ElMessage.success('结账成功')
    showSessionDialog.value = false
    await loadTables()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '结账失败')
  } finally {
    checkoutLoading.value = false
  }
}

const resetSessionDialog = () => {
  currentTable.value = null
  sessionDetail.value = null
  checkoutForm.value = { actualPaid: 0, phone: '' }
  expandedOrders.value = []
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

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

const syncQueryIntent = () => {
  const tableId = Number(route.query.tableId)
  pendingOpenTableId.value = Number.isInteger(tableId) && tableId > 0 ? tableId : null
  openCheckoutDirectly.value = route.query.action === 'checkout'
}

const tryOpenPendingTable = async () => {
  if (!pendingOpenTableId.value) {
    return
  }
  const table = tables.value.find((item) => item.id === pendingOpenTableId.value)
  if (!table || table.status !== 1) {
    pendingOpenTableId.value = null
    return
  }
  const tableId = pendingOpenTableId.value
  const checkoutDirectly = openCheckoutDirectly.value
  pendingOpenTableId.value = null
  openCheckoutDirectly.value = false
  await openSession(table, checkoutDirectly)
  router.replace({ path: '/m/tables-view' })
  if (tableId && checkoutDirectly) {
    await nextTick()
  }
}

watch(
  () => route.query,
  () => {
    syncQueryIntent()
    tryOpenPendingTable()
  },
  { deep: true }
)

onMounted(() => {
  syncQueryIntent()
  loadTables()
  timer.value = setInterval(loadTables, 30000)
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
.session-dialog-body {
  min-height: 120px;
}

.session-order-title {
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding-right: 20px;
  gap: 12px;
}

.table-scroll {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .session-dialog :deep(.el-dialog.is-fullscreen) {
    display: flex;
    flex-direction: column;
  }

  .session-dialog :deep(.el-dialog__body) {
    flex: 1;
    overflow: hidden;
    padding: 0;
  }

  .session-dialog :deep(.el-dialog__footer) {
    border-top: 1px solid #ebeef5;
    background: #fff;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  }

  .session-dialog-body {
    height: 100%;
    overflow-y: auto;
    padding: 16px;
  }

  .session-order-title {
    align-items: flex-start;
    flex-direction: column;
    padding-right: 0;
  }

  .checkout-form :deep(.el-form-item) {
    margin-bottom: 14px;
  }
}
</style>
