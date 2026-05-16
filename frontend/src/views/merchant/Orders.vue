<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span>订单管理</span>
          <div class="filters">
            <el-select
              v-model="filterTableId"
              placeholder="桌台筛选"
              clearable
              style="width: 120px"
              @change="loadOrders"
            >
              <el-option v-for="table in tableList" :key="table.id" :label="table.name" :value="table.id" />
            </el-select>

            <el-select
              v-model="filterStatus"
              placeholder="状态筛选"
              clearable
              style="width: 130px"
              @change="loadOrders"
            >
              <el-option label="待确认" :value="0" />
              <el-option label="制作中" :value="1" />
              <el-option label="部分上菜" :value="2" />
              <el-option label="全部上菜" :value="3" />
              <el-option label="已结账" :value="4" />
              <el-option label="已取消" :value="5" />
            </el-select>

            <el-date-picker
              v-model="dateRange"
              type="daterange"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              style="width: 260px"
              @change="loadOrders"
            />

            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </template>

      <el-collapse v-model="expandedOrders">
        <el-collapse-item v-for="order in pagedOrders" :key="order.id" :name="order.id">
          <template #title>
            <div class="collapse-title">
              <span>{{ order.orderNo }} - {{ order.tableName || '散客' }} - {{ statusText(order.status) }}</span>
              <span class="muted">{{ order.createTime }}</span>
            </div>
          </template>

          <div style="margin-bottom:10px">
            <el-button v-if="order.status === 0" type="primary" size="small" @click="handleConfirm(order.id)">
              确认订单
            </el-button>
            <el-button
              v-if="order.status !== 5 && order.status !== 4"
              type="danger"
              size="small"
              @click="handleCancel(order.id)"
            >
              整单取消
            </el-button>
          </div>

          <div class="table-scroll">
            <el-table :data="order.items" size="small">
              <el-table-column prop="dishName" label="菜品" />
              <el-table-column label="单价(元)" width="90">
                <template #default="{ row }">{{ (row.dishPrice / 100).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="70" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="itemStatusType(row.status)">{{ itemStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 1"
                    size="small"
                    text
                    type="success"
                    @click="openServeDialog(order, row)"
                  >
                    上菜
                  </el-button>
                  <el-button
                    v-if="row.status === 0 || row.status === 1"
                    size="small"
                    text
                    type="danger"
                    @click="handleCancelItem(row.id)"
                  >
                    退菜
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-if="!loading && sortedOrders.length === 0" description="暂无订单" />
      <el-pagination
        v-if="sortedOrders.length > 0"
        style="margin-top:16px; text-align:right"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="sortedOrders.length"
        layout="prev, pager, next"
        @current-change="(value) => currentPage = value"
      />
    </el-card>

    <ServeItemsDialog
      v-model:visible="showServeDialog"
      :order="servingOrder"
      :filter-item-id="servingItemId"
      @served="loadOrders"
    />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList, confirm, cancelItem, cancelOrder } from '../../api/order'
import { list as listTables } from '../../api/table'
import { orderStatusText as statusText, itemStatusText, itemStatusType } from '../../utils/orderStatus'
import ServeItemsDialog from '../../components/ServeItemsDialog.vue'

const route = useRoute()

const loading = ref(false)
const orders = ref([])
const tableList = ref([])
const filterStatus = ref(null)
const filterTableId = ref(null)
const dateRange = ref([])
const expandedOrders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const showServeDialog = ref(false)
const servingOrder = ref(null)
const servingItemId = ref(null)

const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const today = new Date()
      return [today, today]
    }
  },
  {
    text: '昨天',
    value: () => {
      const yesterday = new Date()
      yesterday.setDate(yesterday.getDate() - 1)
      return [yesterday, yesterday]
    }
  },
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end]
    }
  }
]

const statusSortOrder = {
  0: 0,
  1: 1,
  2: 2,
  3: 3,
  4: 4,
  5: 5
}

const sortedOrders = computed(() => {
  return [...orders.value].sort((left, right) => compareOrders(left, right))
})

const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = currentPage.value * pageSize.value
  return sortedOrders.value.slice(start, end)
})

const loadOrders = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value || []
    const res = await merchantList({
      status: filterStatus.value,
      tableId: filterTableId.value,
      startDate,
      endDate
    })
    orders.value = res.data
    currentPage.value = 1
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载订单失败')
  } finally {
    loading.value = false
  }
}

const loadTables = async () => {
  try {
    const res = await listTables()
    tableList.value = res.data
  } catch (e) {
    // ignore
  }
}

const handleConfirm = async (id) => {
  try {
    await confirm(id)
    ElMessage.success('订单已确认')
    loadOrders()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '确认失败')
  }
}

const openServeDialog = (order, item) => {
  servingOrder.value = order
  servingItemId.value = item.id
  showServeDialog.value = true
}

const handleCancelItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定退这道菜品吗？', '提示')
    await cancelItem(itemId)
    ElMessage.success('已退菜')
    loadOrders()
  } catch (e) {
    // cancel
  }
}

const handleCancel = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定取消整单吗？', '提示')
    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) {
    // cancel
  }
}

const resetFilters = () => {
  filterStatus.value = null
  filterTableId.value = null
  dateRange.value = []
  loadOrders()
}

const compareOrders = (left, right) => {
  const leftStatusRank = statusSortOrder[left.status] ?? 99
  const rightStatusRank = statusSortOrder[right.status] ?? 99
  if (leftStatusRank !== rightStatusRank) {
    return leftStatusRank - rightStatusRank
  }

  const timeCompare = compareOrderTime(left.createTime, right.createTime)
  if (timeCompare !== 0) {
    return isFinalStatus(left.status) ? -timeCompare : timeCompare
  }

  return compareTableName(left.tableName, right.tableName)
}

const compareOrderTime = (leftTime, rightTime) => {
  const leftTimestamp = leftTime ? new Date(leftTime.replace(' ', 'T')).getTime() : 0
  const rightTimestamp = rightTime ? new Date(rightTime.replace(' ', 'T')).getTime() : 0
  return leftTimestamp - rightTimestamp
}

const isFinalStatus = (status) => status === 4 || status === 5

const compareTableName = (leftName, rightName) => {
  const leftValue = normalizeTableName(leftName)
  const rightValue = normalizeTableName(rightName)

  const leftNumber = Number(leftValue)
  const rightNumber = Number(rightValue)
  const leftIsNumber = Number.isFinite(leftNumber)
  const rightIsNumber = Number.isFinite(rightNumber)

  if (leftIsNumber && rightIsNumber && leftNumber !== rightNumber) {
    return leftNumber - rightNumber
  }

  return leftValue.localeCompare(rightValue, 'zh-CN', { numeric: true, sensitivity: 'base' })
}

const normalizeTableName = (value) => {
  if (!value || value === '散客') {
    return '999999'
  }
  return String(value).replace(/[^\dA-Za-z\u4e00-\u9fa5]/g, '')
}

const initFiltersFromRoute = () => {
  const routeTableId = Number(route.query.tableId)
  if (Number.isInteger(routeTableId) && routeTableId > 0) {
    filterTableId.value = routeTableId
  }
}

watch(
  () => route.query.tableId,
  (value) => {
    const tableId = Number(value)
    if (Number.isInteger(tableId) && tableId > 0 && tableId !== filterTableId.value) {
      filterTableId.value = tableId
      loadOrders()
    }
  }
)

onMounted(() => {
  initFiltersFromRoute()
  loadTables()
  loadOrders()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.collapse-title {
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding-right: 20px;
  gap: 12px;
}

.muted {
  color: #999;
}

.table-scroll {
  overflow-x: auto;
}
</style>
