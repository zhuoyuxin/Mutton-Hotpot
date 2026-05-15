<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>订单管理</span>
          <div>
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:120px; margin-right:10px" @change="loadOrders">
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
        <el-collapse-item v-for="order in orders" :key="order.id" :name="order.id">
          <template #title>
            <div style="display:flex; justify-content:space-between; width:100%; padding-right:20px">
              <span>{{ order.orderNo }} - {{ statusText(order.status) }}</span>
              <span style="color:#999">{{ order.createTime }}</span>
            </div>
          </template>

          <!-- 操作按钮 -->
          <div style="margin-bottom:10px">
            <el-button v-if="order.status === 0" type="primary" size="small" @click="handleConfirm(order.id)">
              确认订单
            </el-button>
            <el-button v-if="order.status !== 5 && order.status !== 4" type="danger" size="small" @click="handleCancel(order.id)">
              整单取消
            </el-button>
          </div>

          <!-- 菜品列表 -->
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
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" size="small" text type="success" @click="handleServe(row.id)">
                  上菜
                </el-button>
                <el-button v-if="row.status === 0 || row.status === 1" size="small" text type="danger" @click="handleCancelItem(row.id)">
                  退菜
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList, confirm, serveItem, cancelItem, cancelOrder, getItems } from '../../api/order'

const orders = ref([])
const filterStatus = ref(null)
const expandedOrders = ref([])

const statusText = (s) => ['待确认','制作中','部分上菜','全部上菜','已结账','已取消'][s] || ''
const itemStatusText = (s) => ['待确认','待上菜','已上菜','库存不足','已退菜'][s] || ''
const itemStatusType = (s) => ['info','','success','warning','danger'][s] || ''

const loadOrders = async () => {
  const res = await merchantList({ status: filterStatus.value })
  orders.value = res.data
  // 后端 listOrders 已批量返回 items，无需逐个加载
}

const handleConfirm = async (id) => {
  await confirm(id)
  ElMessage.success('订单已确认')
  loadOrders()
}

const handleServe = async (itemId) => {
  await serveItem(itemId)
  ElMessage.success('已上菜')
  loadOrders()
}

const handleCancelItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定退该菜品？', '提示')
    await cancelItem(itemId)
    ElMessage.success('已退菜')
    loadOrders()
  } catch (e) { /* 取消 */ }
}

const handleCancel = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定取消整单？', '提示')
    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) { /* 取消 */ }
}

onMounted(loadOrders)
</script>
