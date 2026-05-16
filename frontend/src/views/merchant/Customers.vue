<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span>客户管理</span>
          <el-input
            v-model="keyword"
            placeholder="搜索手机号或名称"
            :style="isMobile ? 'width:100%' : 'width:240px'"
            clearable
            @keyup.enter="loadCustomers"
          >
            <template #append>
              <el-button @click="loadCustomers">搜索</el-button>
            </template>
          </el-input>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="customers">
          <el-table-column prop="phone" label="手机号" min-width="140" />
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column label="积分" width="100">
            <template #default="{ row }">{{ row.points || 0 }}</template>
          </el-table-column>
          <el-table-column label="累计消费(元)" width="130">
            <template #default="{ row }">{{ formatMoney(row.totalSpent) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click="openDetail(row)">详情</el-button>
              <el-button size="small" text @click="openPoints(row)">积分</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-if="!loading && customers.length === 0" description="暂无客户" />
    </el-card>

    <el-dialog
      v-model="showDetailDialog"
      title="客户详情"
      :width="isMobile ? '95vw' : '840px'"
    >
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <el-descriptions :column="isMobile ? 1 : 2" border size="small">
            <el-descriptions-item label="手机号">{{ detailData.customer?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ detailData.customer?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="积分">{{ detailData.customer?.points || 0 }}</el-descriptions-item>
            <el-descriptions-item label="累计消费">
              {{ formatMoney(detailData.customer?.totalSpent) }} 元
            </el-descriptions-item>
          </el-descriptions>

          <section class="detail-section">
            <div class="section-title">积分明细</div>
            <div class="table-scroll">
                <el-table :data="detailData.pointsRecords || []" size="small" max-height="220">
                <el-table-column prop="remark" label="备注" min-width="180" />
                <el-table-column label="积分变动" width="120">
                  <template #default="{ row }">
                    <span :style="{ color: row.points > 0 ? '#67c23a' : '#f56c6c' }">
                      {{ row.points > 0 ? '+' : '' }}{{ row.points }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="时间" width="180">
                  <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                </el-table-column>
              </el-table>
            </div>
            <el-empty
              v-if="(detailData.pointsRecords || []).length === 0"
              description="暂无积分记录"
            />
          </section>

          <section class="detail-section">
            <div class="section-title">消费记录（按一次就餐汇总）</div>
            <el-collapse v-if="consumptionRecords.length > 0" v-model="activeRecords">
              <el-collapse-item
                v-for="record in consumptionRecords"
                :key="record.checkoutId || record.sessionId"
                :name="record.checkoutId || record.sessionId"
              >
                <template #title>
                  <div class="record-title">
                    <div class="record-title-main">
                      <span>{{ record.tableName || '散台' }}</span>
                      <span v-if="record.tableArea" class="muted">/ {{ record.tableArea }}</span>
                      <el-tag size="small" type="success">实收 {{ formatMoney(record.actualPaid) }} 元</el-tag>
                    </div>
                    <div class="muted">{{ formatDateTime(record.checkoutTime) }}</div>
                  </div>
                </template>

                <el-descriptions :column="isMobile ? 1 : 2" border size="small">
                  <el-descriptions-item label="桌台">{{ formatTableLabel(record) }}</el-descriptions-item>
                  <el-descriptions-item label="结账时间">{{ formatDateTime(record.checkoutTime) }}</el-descriptions-item>
                  <el-descriptions-item label="订单数">{{ record.orderCount || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="菜品份数">{{ consumptionDishCount(record) }}</el-descriptions-item>
                  <el-descriptions-item label="应收金额">{{ formatMoney(record.totalAmount) }} 元</el-descriptions-item>
                  <el-descriptions-item label="实收金额">{{ formatMoney(record.actualPaid) }} 元</el-descriptions-item>
                  <el-descriptions-item label="优惠金额">{{ formatMoney(record.discountAmount) }} 元</el-descriptions-item>
                  <el-descriptions-item label="本次积分">{{ record.pointsEarned || 0 }}</el-descriptions-item>
                </el-descriptions>

                <div class="subsection-title">本次菜品汇总</div>
                <div class="table-scroll">
                  <el-table :data="record.dishSummary || []" size="small">
                    <el-table-column prop="dishName" label="菜品" min-width="180" />
                    <el-table-column label="单价(元)" width="110">
                      <template #default="{ row }">{{ formatMoney(row.dishPrice) }}</template>
                    </el-table-column>
                    <el-table-column prop="quantity" label="份数" width="80" />
                    <el-table-column label="金额(元)" width="110">
                      <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
                    </el-table-column>
                  </el-table>
                </div>
                <el-empty
                  v-if="(record.dishSummary || []).length === 0"
                  description="本次没有可统计的菜品"
                />

                <div class="subsection-title">包含订单</div>
                <div class="table-scroll">
                  <el-table :data="record.orders || []" size="small">
                    <el-table-column prop="orderNo" label="订单号" min-width="180" />
                    <el-table-column label="菜品摘要" min-width="220">
                      <template #default="{ row }">{{ orderDishSummary(row) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="110">
                      <template #default="{ row }">
                        <el-tag size="small" :type="orderStatusType(row.status)">
                          {{ orderStatusText(row.status) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="下单时间" width="180">
                      <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                    </el-table-column>
                  </el-table>
                </div>
                <el-empty
                  v-if="(record.orders || []).length === 0"
                  description="本次没有关联订单"
                />
              </el-collapse-item>
            </el-collapse>

            <el-empty v-else description="暂无消费记录" />
          </section>
        </template>
      </div>
    </el-dialog>

    <el-dialog
      v-model="showPointsDialog"
      title="手动调整积分"
      :width="isMobile ? '92vw' : '420px'"
    >
      <p>客户：{{ currentCustomer?.name || '-' }}（当前积分：{{ currentCustomer?.points || 0 }}）</p>
      <el-input-number v-model="pointsForm.points" style="width:100%; margin-top:10px" />
      <p class="muted helper-text">正数为增加，负数为扣减。</p>
      <el-input
        v-model="pointsForm.remark"
        placeholder="备注原因"
        style="margin-top:10px"
      />
      <template #footer>
        <el-button @click="showPointsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSavePoints">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantDetail, merchantList, manualPoints } from '../../api/customer'
import { formatPrice } from '../../utils/format'
import { orderStatusText, orderStatusType } from '../../utils/orderStatus'

const loading = ref(false)
const detailLoading = ref(false)
const customers = ref([])
const keyword = ref('')
const showPointsDialog = ref(false)
const showDetailDialog = ref(false)
const detailData = ref(null)
const currentCustomer = ref(null)
const activeRecords = ref([])
const pointsForm = ref({ customerId: null, points: 0, remark: '' })
const isMobile = ref(window.innerWidth <= 768)

const consumptionRecords = computed(() => detailData.value?.consumptionRecords || [])

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  loadCustomers()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const loadCustomers = async () => {
  loading.value = true
  try {
    const res = await merchantList({ keyword: keyword.value })
    customers.value = res.data || []
  } catch (e) {
    ElMessage.error('加载客户数据失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  detailData.value = null
  detailLoading.value = true
  activeRecords.value = []
  showDetailDialog.value = true

  try {
    const res = await merchantDetail(row.id)
    detailData.value = res.data
    const firstRecord = (res.data?.consumptionRecords || [])[0]
    activeRecords.value = firstRecord ? [firstRecord.checkoutId || firstRecord.sessionId] : []
  } catch (e) {
    showDetailDialog.value = false
    ElMessage.error('加载客户详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openPoints = (row) => {
  currentCustomer.value = row
  pointsForm.value = { customerId: row.id, points: 0, remark: '' }
  showPointsDialog.value = true
}

const handleSavePoints = async () => {
  try {
    await manualPoints(pointsForm.value)
    ElMessage.success('积分调整成功')
    showPointsDialog.value = false
    await loadCustomers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '积分调整失败')
  }
}

const formatMoney = (value) => formatPrice(Number(value) || 0)

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').split('.')[0]
}

const formatTableLabel = (record) => {
  const name = record?.tableName || '散台'
  return record?.tableArea ? `${record.tableArea} / ${name}` : name
}

const consumptionDishCount = (record) => {
  return (record?.dishSummary || []).reduce((total, item) => total + (item.quantity || 0), 0)
}

const orderDishSummary = (order) => {
  if (!order?.items || order.items.length === 0) {
    return '-'
  }
  return order.items.map((item) => `${item.dishName} x${item.quantity}`).join('、')
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-section {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}

.subsection-title {
  margin: 14px 0 8px;
  font-size: 13px;
  font-weight: 600;
}

.record-title {
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding-right: 20px;
  gap: 12px;
}

.record-title-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.muted {
  color: #909399;
}

.helper-text {
  margin-top: 6px;
  font-size: 12px;
}

.table-scroll {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .record-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
