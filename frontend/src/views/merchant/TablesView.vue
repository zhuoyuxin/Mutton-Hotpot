<template>
  <div v-loading="loading" class="tables-page">
    <el-card class="overview-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">桌台运营</div>
            <div class="card-subtitle">查看桌台状态、订单进度，并完成结账收银。</div>
          </div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col
          v-for="table in tables"
          :key="table.id"
          :xs="12"
          :sm="8"
          :md="6"
          class="table-col"
        >
          <el-card
            shadow="hover"
            class="table-card"
            :style="{ borderLeft: table.status === 1 ? '4px solid #d97745' : '4px solid #5f9f78' }"
          >
            <div class="table-card-head">
              <span class="table-name">{{ table.name }}</span>
              <el-tag :type="table.status === 1 ? 'warning' : 'success'" size="small">
                {{ table.status === 1 ? '使用中' : '空闲' }}
              </el-tag>
            </div>

            <div v-if="table.area" class="table-area">{{ table.area }}</div>

            <template v-if="table.status === 1 && table.orders && table.orders.length > 0">
              <div class="table-progress">
                <span class="progress-served">已上 {{ table.servedItems }}</span>
                <span>/</span>
                <span class="progress-pending">待处理 {{ table.pendingItems }}</span>
                <span>/</span>
                <span>共 {{ table.totalItems }} 项</span>
              </div>
              <div class="table-summary">{{ latestOrderSummary(table) || '暂无菜品摘要' }}</div>
            </template>

            <template v-else>
              <div class="table-summary empty">当前桌台空闲，可等待顾客扫码点餐。</div>
            </template>

            <div class="table-actions">
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
                直接结账
              </el-button>
              <el-button
                v-if="table.status === 1"
                size="small"
                @click="goOrders(table.id)"
              >
                查看订单
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
      :width="isMobile ? '92vw' : '860px'"
      @closed="resetSessionDialog"
    >
      <div v-if="sessionDetail" v-loading="detailLoading" class="session-dialog-body">
        <div class="session-header">
          <div>
            <div class="session-table-name">
              {{ currentTable?.name }}
              <span v-if="currentTable?.area" class="session-area">/ {{ currentTable.area }}</span>
            </div>
            <div class="session-meta">会话 ID：{{ sessionDetail.id }}</div>
          </div>
          <div class="session-total">
            <span class="session-total-label">当前菜品金额</span>
            <strong>{{ formatFen(sessionDetail.dishAmount) }} 元</strong>
          </div>
        </div>

        <div class="summary-grid">
          <div class="summary-card">
            <span class="summary-label">菜品金额</span>
            <strong>{{ formatFen(sessionDetail.dishAmount) }} 元</strong>
          </div>
          <div class="summary-card warm">
            <span class="summary-label">自助费</span>
            <strong>{{ formatFen(selfServiceAmountFen) }} 元</strong>
            <span class="summary-meta">{{ checkoutForm.selfServiceCount || 0 }} 人</span>
          </div>
          <div class="summary-card neutral">
            <span class="summary-label">餐具费</span>
            <strong>{{ formatFen(tablewareAmountFen) }} 元</strong>
            <span class="summary-meta">{{ checkoutForm.tablewareCount || 0 }} 人</span>
          </div>
          <div class="summary-card dark">
            <span class="summary-label">应收合计</span>
            <strong>{{ formatFen(computedReceivableFen) }} 元</strong>
            <span class="summary-meta">系统自动计算</span>
          </div>
        </div>

        <el-alert
          type="warning"
          :closable="false"
          show-icon
          class="fee-alert"
          title="自助费包含蘸料和蔬菜畅吃，结账按实际人数确认；餐具费单独按人头计算。"
        />

        <el-collapse v-model="expandedOrders">
          <el-collapse-item
            v-for="order in sessionDetail.orders"
            :key="order.id"
            :name="order.id"
          >
            <template #title>
              <div class="session-order-title">
                <span>{{ order.orderNo }}</span>
                <span class="session-order-time">{{ order.createTime }}</span>
              </div>
            </template>

            <div class="table-scroll">
              <el-table :data="order.items || []" size="small">
                <el-table-column prop="dishName" label="菜品" />
                <el-table-column prop="quantity" label="数量" width="70" />
                <el-table-column label="单价(元)" width="96">
                  <template #default="{ row }">{{ formatFen(row.dishPrice) }}</template>
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

        <div class="fee-grid">
          <div class="fee-panel">
            <div class="fee-panel-title">自助费</div>
            <div class="fee-panel-subtitle">蘸料与蔬菜畅吃按人头收费</div>
            <el-form-item label="人数">
              <el-input-number
                v-model="checkoutForm.selfServiceCount"
                :min="0"
                :precision="0"
                :step="1"
                style="width:100%"
              />
            </el-form-item>
            <el-form-item label="单价(元)">
              <el-input-number
                v-model="checkoutForm.selfServiceUnitPrice"
                :min="0"
                :precision="2"
                :step="1"
                style="width:100%"
              />
            </el-form-item>
            <div class="fee-panel-total">小计 {{ formatFen(selfServiceAmountFen) }} 元</div>
          </div>

          <div class="fee-panel">
            <div class="fee-panel-title">餐具费</div>
            <div class="fee-panel-subtitle">如有免收，可将人数改为 0</div>
            <el-form-item label="人数">
              <el-input-number
                v-model="checkoutForm.tablewareCount"
                :min="0"
                :precision="0"
                :step="1"
                style="width:100%"
              />
            </el-form-item>
            <el-form-item label="单价(元)">
              <el-input-number
                v-model="checkoutForm.tablewareUnitPrice"
                :min="0"
                :precision="2"
                :step="1"
                style="width:100%"
              />
            </el-form-item>
            <div class="fee-panel-total">小计 {{ formatFen(tablewareAmountFen) }} 元</div>
          </div>
        </div>

        <el-form
          :model="checkoutForm"
          :label-width="isMobile ? 'auto' : '110px'"
          :label-position="isMobile ? 'top' : 'right'"
          class="checkout-form"
        >
          <el-form-item label="应收合计">
            <div class="receivable-box">
              <span class="receivable-value">{{ formatFen(computedReceivableFen) }} 元</span>
              <el-button size="small" @click="syncActualPaidToReceivable(true)">带入实收</el-button>
            </div>
          </el-form-item>
          <el-form-item label="实收金额(元)">
            <el-input-number
              v-model="checkoutForm.actualPaid"
              :min="0"
              :precision="2"
              :step="1"
              style="width:100%"
              @change="handleActualPaidChange"
            />
          </el-form-item>
          <el-form-item label="手机号(选填)">
            <el-input v-model="checkoutForm.phone" placeholder="填写手机号可累计积分并关联顾客" />
          </el-form-item>
        </el-form>

        <div class="checkout-footnote">
          预计优惠金额：{{ formatFen(discountPreviewFen) }} 元
        </div>
      </div>

      <template #footer>
        <el-button @click="showSessionDialog = false">关闭</el-button>
        <el-button type="primary" :loading="checkoutLoading" @click="handleCheckout">确认结账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
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
const checkoutForm = ref(createCheckoutForm())
const expandedOrders = ref([])
const pendingOpenTableId = ref(null)
const openCheckoutDirectly = ref(false)
const actualPaidDirty = ref(false)
const syncingActualPaid = ref(false)

function createCheckoutForm(detailData = null) {
  return {
    actualPaid: detailData ? fenToYuan(detailData.dishAmount || detailData.totalAmount || 0) : 0,
    phone: '',
    selfServiceCount: 0,
    selfServiceUnitPrice: detailData ? fenToYuan(detailData.defaultSelfServiceUnitPrice || 0) : 0,
    tablewareCount: 0,
    tablewareUnitPrice: detailData ? fenToYuan(detailData.defaultTablewareUnitPrice || 0) : 0
  }
}

const selfServiceAmountFen = computed(() => {
  return integerCount(checkoutForm.value.selfServiceCount) * yuanToFen(checkoutForm.value.selfServiceUnitPrice)
})

const tablewareAmountFen = computed(() => {
  return integerCount(checkoutForm.value.tablewareCount) * yuanToFen(checkoutForm.value.tablewareUnitPrice)
})

const computedReceivableFen = computed(() => {
  const dishAmount = Number(sessionDetail.value?.dishAmount || sessionDetail.value?.totalAmount || 0)
  return dishAmount + selfServiceAmountFen.value + tablewareAmountFen.value
})

const discountPreviewFen = computed(() => {
  const actualPaidFen = yuanToFen(checkoutForm.value.actualPaid)
  return Math.max(0, computedReceivableFen.value - actualPaidFen)
})

function integerCount(value) {
  return Math.max(0, Number(value || 0))
}

function yuanToFen(value) {
  return Math.max(0, Math.round(Number(value || 0) * 100))
}

function fenToYuan(value) {
  return Number((Number(value || 0) / 100).toFixed(2))
}

function formatFen(value) {
  return fenToYuan(value).toFixed(2)
}

function syncActualPaidToReceivable(force = false) {
  if (!sessionDetail.value) {
    return
  }
  if (actualPaidDirty.value && !force) {
    return
  }
  syncingActualPaid.value = true
  checkoutForm.value.actualPaid = fenToYuan(computedReceivableFen.value)
  syncingActualPaid.value = false
  if (force) {
    actualPaidDirty.value = false
  }
}

function handleActualPaidChange(value) {
  if (syncingActualPaid.value) {
    return
  }
  actualPaidDirty.value = yuanToFen(value) !== computedReceivableFen.value
}

watch(
  () => [
    checkoutForm.value.selfServiceCount,
    checkoutForm.value.selfServiceUnitPrice,
    checkoutForm.value.tablewareCount,
    checkoutForm.value.tablewareUnitPrice
  ],
  () => {
    syncActualPaidToReceivable()
  }
)

const loadTables = async () => {
  loading.value = true
  try {
    const res = await overview()
    tables.value = res.data || []
    await tryOpenPendingTable()
  } catch (error) {
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
    const data = detailRes.data || {}
    sessionDetail.value = {
      ...data.session,
      orders: data.orders || [],
      dishAmount: Number(data.dishAmount || data.totalAmount || 0),
      totalAmount: Number(data.totalAmount || data.dishAmount || 0),
      defaultSelfServiceUnitPrice: Number(data.defaultSelfServiceUnitPrice || 0),
      defaultTablewareUnitPrice: Number(data.defaultTablewareUnitPrice || 0)
    }
    expandedOrders.value = checkoutDirectly
      ? []
      : sessionDetail.value.orders.map((order) => order.id)

    checkoutForm.value = createCheckoutForm(sessionDetail.value)
    actualPaidDirty.value = false
    syncActualPaidToReceivable(true)
    showSessionDialog.value = true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载会话详情失败')
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
    await checkout(sessionDetail.value.id, {
      actualPaid: Number(checkoutForm.value.actualPaid || 0),
      selfServiceCount: integerCount(checkoutForm.value.selfServiceCount),
      selfServiceUnitPrice: Number(checkoutForm.value.selfServiceUnitPrice || 0),
      tablewareCount: integerCount(checkoutForm.value.tablewareCount),
      tablewareUnitPrice: Number(checkoutForm.value.tablewareUnitPrice || 0),
      phone: checkoutForm.value.phone || ''
    })
    ElMessage.success('结账成功')
    showSessionDialog.value = false
    await loadTables()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '结账失败')
  } finally {
    checkoutLoading.value = false
  }
}

const resetSessionDialog = () => {
  currentTable.value = null
  sessionDetail.value = null
  checkoutForm.value = createCheckoutForm()
  expandedOrders.value = []
  actualPaidDirty.value = false
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
  const checkoutDirectly = openCheckoutDirectly.value
  pendingOpenTableId.value = null
  openCheckoutDirectly.value = false
  await openSession(table, checkoutDirectly)
  router.replace({ path: '/m/tables-view' })
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
.table-col {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2f241f;
}

.card-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #8a7468;
}

.table-card {
  min-height: 184px;
}

.table-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.table-name {
  font-weight: 700;
  color: #2f241f;
}

.table-area {
  margin-top: 4px;
  font-size: 12px;
  color: #8d857e;
}

.table-progress {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 13px;
  color: #6b5b52;
}

.progress-served {
  color: #4f8a63;
}

.progress-pending {
  color: #d97745;
}

.table-summary {
  min-height: 38px;
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #75655a;
}

.table-summary.empty {
  color: #9c9087;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.session-dialog-body {
  min-height: 120px;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.session-table-name {
  font-size: 18px;
  font-weight: 700;
  color: #2f241f;
}

.session-area,
.session-meta,
.session-order-time {
  color: #8a7d73;
}

.session-meta {
  margin-top: 6px;
  font-size: 12px;
}

.session-total {
  text-align: right;
}

.session-total-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #8a7d73;
}

.session-total strong {
  font-size: 20px;
  color: #ba5a2c;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: 16px;
  background: #f7efe7;
  color: #2f241f;
}

.summary-card.warm {
  background: linear-gradient(180deg, #fff5eb, #f7e2cf);
}

.summary-card.neutral {
  background: linear-gradient(180deg, #f8f2ed, #efe4d9);
}

.summary-card.dark {
  background: linear-gradient(180deg, #3e2b25, #241915);
  color: #fff3e5;
}

.summary-label {
  font-size: 12px;
  color: inherit;
  opacity: 0.72;
}

.summary-card strong {
  font-size: 22px;
  line-height: 1.1;
}

.summary-meta {
  font-size: 12px;
  opacity: 0.72;
}

.fee-alert {
  margin-bottom: 18px;
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

.fee-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.fee-panel {
  padding: 16px;
  border-radius: 18px;
  background: #faf4ee;
  border: 1px solid rgba(175, 129, 94, 0.14);
}

.fee-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #2f241f;
}

.fee-panel-subtitle {
  margin: 6px 0 14px;
  font-size: 12px;
  color: #8b7d72;
}

.fee-panel-total {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #b35c31;
}

.receivable-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.receivable-value {
  font-size: 18px;
  font-weight: 700;
  color: #b35c31;
}

.checkout-footnote {
  margin-top: 4px;
  font-size: 12px;
  color: #8b7d72;
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

  .session-header,
  .session-order-title,
  .receivable-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .session-order-title {
    padding-right: 0;
  }

  .summary-grid,
  .fee-grid {
    grid-template-columns: 1fr;
  }
}
</style>
