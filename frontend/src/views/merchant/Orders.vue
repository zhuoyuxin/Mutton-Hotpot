<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px">
          <span>订单管理</span>
          <div style="display:flex; gap:8px; flex-wrap:wrap">
            <el-select v-model="filterTableId" placeholder="桌台筛选" clearable style="width:120px" @change="loadOrders">
              <el-option v-for="t in tableList" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:120px" @change="loadOrders">
              <el-option label="待确认" :value="0" />
              <el-option label="制作中" :value="1" />
              <el-option label="部分上菜" :value="2" />
              <el-option label="全部上菜" :value="3" />
              <el-option label="已取消" :value="5" />
            </el-select>
          </div>
        </div>
      </template>

      <el-collapse v-model="expandedOrders">
        <el-collapse-item v-for="order in pagedOrders" :key="order.id" :name="order.id">
          <template #title>
            <div style="display:flex; justify-content:space-between; width:100%; padding-right:20px">
              <span>{{ order.orderNo }} - {{ order.tableName || '散客' }} - {{ statusText(order.status) }}</span>
              <span style="color:#999">{{ order.createTime }}</span>
            </div>
          </template>

          <div style="margin-bottom:10px">
            <el-button v-if="order.status === 0" type="primary" size="small" @click="handleConfirm(order.id)">
              确认订单
            </el-button>
            <el-button v-if="order.status !== 5 && order.status !== 4" type="danger" size="small" @click="handleCancel(order.id)">
              整单取消
            </el-button>
          </div>

          <div class="table-scroll">
            <el-table :data="order.items" size="small">
              <el-table-column prop="dishName" label="菜品" />
              <el-table-column label="单价(元)" width="80">
                <template #default="{ row }">{{ (row.dishPrice / 100).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="60" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="itemStatusType(row.status)">{{ itemStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button v-if="row.status === 1" size="small" text type="success" @click="openServeDialog(order, row)">
                    上菜
                  </el-button>
                  <el-button v-if="row.status === 0 || row.status === 1" size="small" text type="danger" @click="handleCancelItem(row.id)">
                    退菜
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
      <el-pagination
        v-if="orders.length > 0"
        style="margin-top:16px; text-align:right"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="orders.length"
        layout="prev, pager, next"
        @current-change="(val) => currentPage = val"
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList, confirm, cancelItem, cancelOrder } from '../../api/order'
import { list as listTables } from '../../api/table'
import { orderStatusText as statusText, itemStatusText, itemStatusType } from '../../utils/orderStatus'
import ServeItemsDialog from '../../components/ServeItemsDialog.vue'

const loading = ref(false)
const orders = ref([])
const tableList = ref([])
const filterStatus = ref(null)
const filterTableId = ref(null)
const expandedOrders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const showServeDialog = ref(false)
const servingOrder = ref(null)
const servingItemId = ref(null)

const pagedOrders = computed(() =>
  orders.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value)
)

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await merchantList({ status: filterStatus.value, tableId: filterTableId.value })
    orders.value = res.data
    currentPage.value = 1
  } catch (e) {
    ElMessage.error('加载订单失败')
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
    ElMessage.error('确认失败')
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

onMounted(() => {
  loadTables()
  loadOrders()
})
</script>

<style scoped>
.table-scroll {
  overflow-x: auto;
}
</style>
