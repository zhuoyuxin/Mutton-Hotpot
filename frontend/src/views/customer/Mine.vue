<template>
  <div class="customer-page">
    <!-- 用户信息 -->
    <div class="user-card">
      <h3>{{ info.name || '未登录' }}</h3>
      <p v-if="info.phone">{{ maskedPhone }}</p>
      <div class="stats">
        <div class="stat-item">
          <div class="stat-val">{{ info.points || 0 }}</div>
          <div class="stat-label">积分</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">{{ formatPrice(info.totalSpent || 0) }}</div>
          <div class="stat-label">累计消费(元)</div>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" style="background:#fff; margin-top:10px">
      <el-tab-pane label="消费记录" name="orders">
        <div v-for="order in orders" :key="order.id" class="order-item">
          <div style="display:flex; justify-content:space-between">
            <span>{{ order.orderNo }}</span>
            <span style="color:#f56c6c">&yen;{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <div style="color:#999; font-size:12px; margin-top:4px">{{ order.createTime }}</div>
        </div>
        <div v-if="orders.length === 0" style="text-align:center; padding:30px; color:#999">暂无消费记录</div>
      </el-tab-pane>

      <el-tab-pane label="积分明细" name="points">
        <div v-for="pr in pointsRecords" :key="pr.id" class="order-item">
          <div style="display:flex; justify-content:space-between">
            <span>{{ pr.remark }}</span>
            <span :style="{ color: pr.points > 0 ? '#67c23a' : '#f56c6c' }">
              {{ pr.points > 0 ? '+' : '' }}{{ pr.points }}
            </span>
          </div>
          <div style="color:#999; font-size:12px; margin-top:4px">{{ pr.createTime }}</div>
        </div>
        <div v-if="pointsRecords.length === 0" style="text-align:center; padding:30px; color:#999">暂无积分记录</div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { customerInfo, customerOrders, customerPoints } from '../../api/customer'
import { formatPrice } from '../../utils/format'

const info = ref({})
const orders = ref([])
const pointsRecords = ref([])
const activeTab = ref('orders')

const maskedPhone = computed(() => info.value.phone ? info.value.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '')

const loadData = async () => {
  try {
    const [infoRes, ordersRes, pointsRes] = await Promise.allSettled([
      customerInfo(),
      customerOrders(),
      customerPoints()
    ])
    if (infoRes.status === 'fulfilled') { info.value = infoRes.value.data || {} }
    if (ordersRes.status === 'fulfilled') { orders.value = ordersRes.value.data || [] }
    if (pointsRes.status === 'fulfilled') { pointsRecords.value = pointsRes.value.data || [] }
  } catch (e) { /* 未登录等情况 */ }
}

onMounted(loadData)
</script>

<style scoped>
.customer-page { min-height: 100vh; background: #f5f5f5; }
.user-card {
  background: linear-gradient(135deg, #f56c6c, #e6393d);
  color: #fff; padding: 30px 20px; text-align: center;
}
.user-card h3 { margin: 0 0 5px; }
.user-card p { margin: 0; opacity: 0.8; font-size: 14px; }
.stats { display: flex; justify-content: center; gap: 40px; margin-top: 20px; }
.stat-item { text-align: center; }
.stat-val { font-size: 24px; font-weight: 500; }
.stat-label { font-size: 12px; opacity: 0.8; margin-top: 4px; }
.order-item { padding: 12px 15px; border-bottom: 1px solid #f5f5f5; }
</style>
