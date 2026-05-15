<template>
  <div class="customer-page">
    <h2 style="text-align:center; padding:15px 0; background:#fff">上菜状态</h2>

    <div v-if="loadError" style="text-align:center; padding:40px; color:#f56c6c">
      <p>加载失败，请稍后重试</p>
      <el-button type="primary" size="small" @click="loadData" style="margin-top:10px">重试</el-button>
    </div>

    <div v-else-if="orders.length === 0" style="text-align:center; padding:40px; color:#999">
      暂无订单
    </div>

    <div v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <span>{{ order.orderNo }}</span>
        <el-tag size="small">{{ orderStatusText(order.status) }}</el-tag>
      </div>

      <div v-for="item in order.items" :key="item.id" class="item-row">
        <span class="item-name">{{ item.dishName }}</span>
        <span class="item-qty">x{{ item.quantity }}</span>
        <el-tag size="small" :type="itemStatusType(item.status)">{{ itemStatusText(item.status) }}</el-tag>
      </div>
    </div>

    <div style="text-align:center; padding:20px">
      <el-button type="danger" @click="$router.push('/c/order/' + $route.params.tableId)">继续点餐</el-button>
      <el-button @click="$router.push('/c/mine')">我的</el-button>
    </div>

    <!-- 自动刷新 -->
    <div style="text-align:center; color:#999; font-size:12px; padding:10px">
      每 10 秒自动刷新
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { tableOrders } from '../../api/order'

const route = useRoute()
const orders = ref([])
const loadError = ref(false)
let timer = null

const orderStatusText = (s) => ['待确认','制作中','部分上菜','全部上菜','已结账','已取消'][s] || ''
const itemStatusText = (s) => ['待确认','待上菜','已上菜','库存不足','已退菜'][s] || ''
const itemStatusType = (s) => ['info','warning','success','danger','info'][s] || ''

const loadData = async () => {
  try {
    loadError.value = false
    const res = await tableOrders(route.params.tableId)
    orders.value = res.data
  } catch (e) {
    loadError.value = true
  }
}

const handleVisibility = () => {
  if (document.hidden) {
    clearInterval(timer)
    timer = null
  } else {
    loadData()
    timer = setInterval(loadData, 10000)
  }
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 10000)
  document.addEventListener('visibilitychange', handleVisibility)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('visibilitychange', handleVisibility)
})
</script>

<style scoped>
.customer-page { min-height: 100vh; background: #f5f5f5; }
.order-card { background: #fff; margin: 10px; border-radius: 8px; padding: 15px; }
.order-header { display: flex; justify-content: space-between; margin-bottom: 10px; font-weight: 500; }
.item-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
.item-name { flex: 1; }
.item-qty { width: 40px; text-align: center; color: #666; }
</style>
