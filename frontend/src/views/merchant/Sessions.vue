<template>
  <div v-loading="loading" class="sessions-page">
    <el-card>
      <template #header>
        <div class="header-block">
          <div>
            <div class="page-title">会话结账</div>
            <div class="page-subtitle">按桌台快速打开进行中的会话，并确认自助费、餐具费与最终实收。</div>
          </div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col
          v-for="table in busyTables"
          :key="table.id"
          :xs="12"
          :sm="8"
          :md="6"
          class="table-col"
        >
          <el-card shadow="hover" class="busy-card" @click="openSession(table)">
            <div class="busy-card-content">
              <h3>{{ table.name }}</h3>
              <el-tag type="warning">{{ table.area || '堂食区' }} / 使用中</el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && busyTables.length === 0" description="暂无使用中的桌台" />
    </el-card>

    <el-dialog
      v-model="showCheckout"
      title="整桌结账"
      :width="isMobile ? '92vw' : '720px'"
    >
      <div v-if="sessionDetail" v-loading="detailLoading">
        <div class="summary-grid">
          <div class="summary-card">
            <span>菜品金额</span>
            <strong>{{ formatFen(sessionDetail.dishAmount) }} 元</strong>
          </div>
          <div class="summary-card warm">
            <span>自助费</span>
            <strong>{{ formatFen(selfServiceAmountFen) }} 元</strong>
            <small>{{ checkoutForm.selfServiceCount || 0 }} 人</small>
          </div>
          <div class="summary-card neutral">
            <span>餐具费</span>
            <strong>{{ formatFen(tablewareAmountFen) }} 元</strong>
            <small>{{ checkoutForm.tablewareCount || 0 }} 人</small>
          </div>
          <div class="summary-card dark">
            <span>应收合计</span>
            <strong>{{ formatFen(computedReceivableFen) }} 元</strong>
          </div>
        </div>

        <el-alert
          type="warning"
          :closable="false"
          show-icon
          class="fee-alert"
          title="自助费包含蘸料与蔬菜畅吃，最终按实际人数结算。"
        />

        <el-descriptions title="订单明细" :column="1" border size="small">
          <template v-for="order in sessionDetail.orders" :key="order.id">
            <el-descriptions-item :label="order.orderNo">
              <span
                v-for="item in order.items || []"
                :key="item.id"
                class="order-item-line"
              >
                {{ item.dishName }} x{{ item.quantity }}
                <el-tag size="small" :type="itemStatusType(item.status)">{{ itemStatusText(item.status) }}</el-tag>
              </span>
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <div class="fee-grid">
          <div class="fee-panel">
            <div class="fee-panel-title">自助费</div>
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
          </div>

          <div class="fee-panel">
            <div class="fee-panel-title">餐具费</div>
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
          </div>
        </div>

        <el-form :model="checkoutForm" label-width="110px" class="checkout-form">
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
      </div>

      <template #footer>
        <el-button @click="showCheckout = false">取消</el-button>
        <el-button type="primary" @click="handleCheckout" :loading="loading">确认结账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listTables } from '../../api/table'
import { current, detail, checkout } from '../../api/session'
import { itemStatusText, itemStatusType } from '../../utils/orderStatus'

const loading = ref(false)
const detailLoading = ref(false)
const busyTables = ref([])
const showCheckout = ref(false)
const sessionDetail = ref(null)
const checkoutForm = ref(createCheckoutForm())
const isMobile = ref(window.innerWidth <= 768)
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
  return Math.max(0, Number(checkoutForm.value.selfServiceCount || 0)) * yuanToFen(checkoutForm.value.selfServiceUnitPrice)
})

const tablewareAmountFen = computed(() => {
  return Math.max(0, Number(checkoutForm.value.tablewareCount || 0)) * yuanToFen(checkoutForm.value.tablewareUnitPrice)
})

const computedReceivableFen = computed(() => {
  const dishAmount = Number(sessionDetail.value?.dishAmount || sessionDetail.value?.totalAmount || 0)
  return dishAmount + selfServiceAmountFen.value + tablewareAmountFen.value
})

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
  () => syncActualPaidToReceivable()
)

const handleResize = () => { isMobile.value = window.innerWidth <= 768 }

onMounted(() => {
  loadTables()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const loadTables = async () => {
  loading.value = true
  try {
    const res = await listTables()
    busyTables.value = (res.data || []).filter((table) => table.status === 1)
  } catch (error) {
    ElMessage.error('加载桌台数据失败')
  } finally {
    loading.value = false
  }
}

const openSession = async (table) => {
  detailLoading.value = true
  try {
    const sessRes = await current(table.id)
    if (!sessRes.data) {
      ElMessage.warning('该桌台没有进行中的会话')
      return
    }
    const detailRes = await detail(sessRes.data.id)
    const data = detailRes.data || {}
    sessionDetail.value = {
      ...data.session,
      orders: data.orders || [],
      dishAmount: Number(data.dishAmount || data.totalAmount || 0),
      totalAmount: Number(data.totalAmount || data.dishAmount || 0),
      defaultSelfServiceUnitPrice: Number(data.defaultSelfServiceUnitPrice || 0),
      defaultTablewareUnitPrice: Number(data.defaultTablewareUnitPrice || 0)
    }
    checkoutForm.value = createCheckoutForm(sessionDetail.value)
    actualPaidDirty.value = false
    syncActualPaidToReceivable(true)
    showCheckout.value = true
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

  loading.value = true
  try {
    await checkout(sessionDetail.value.id, {
      actualPaid: Number(checkoutForm.value.actualPaid || 0),
      selfServiceCount: Math.max(0, Number(checkoutForm.value.selfServiceCount || 0)),
      selfServiceUnitPrice: Number(checkoutForm.value.selfServiceUnitPrice || 0),
      tablewareCount: Math.max(0, Number(checkoutForm.value.tablewareCount || 0)),
      tablewareUnitPrice: Number(checkoutForm.value.tablewareUnitPrice || 0),
      phone: checkoutForm.value.phone || ''
    })
    ElMessage.success('结账成功')
    showCheckout.value = false
    await loadTables()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '结账失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.header-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.table-col {
  margin-bottom: 16px;
}

.busy-card {
  cursor: pointer;
}

.busy-card-content {
  text-align: center;
}

.busy-card-content h3 {
  margin: 8px 0 12px;
  color: #2f241f;
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

.summary-card span,
.summary-card small {
  opacity: 0.72;
}

.summary-card strong {
  font-size: 20px;
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

.fee-alert {
  margin-bottom: 16px;
}

.order-item-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 10px;
  margin-bottom: 6px;
}

.fee-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 18px 0;
}

.fee-panel {
  padding: 16px;
  border-radius: 18px;
  background: #faf4ee;
  border: 1px solid rgba(175, 129, 94, 0.14);
}

.fee-panel-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 700;
  color: #2f241f;
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

@media (max-width: 768px) {
  .summary-grid,
  .fee-grid {
    grid-template-columns: 1fr;
  }

  .receivable-box {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
