<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <span>会话结账</span>
      </template>
      <el-row :gutter="16">
        <el-col v-for="table in busyTables" :key="table.id" :xs="12" :sm="8" :md="6" style="margin-bottom:16px">
          <el-card shadow="hover" @click="openSession(table)" style="cursor:pointer">
            <div style="text-align:center">
              <h3>{{ table.name }}</h3>
              <el-tag type="warning">{{ table.area }} - 使用中</el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && busyTables.length === 0" description="暂无使用中的桌台" />
    </el-card>

    <!-- 结账弹窗 -->
    <el-dialog v-model="showCheckout" title="整桌结账" :width="isMobile ? '92vw' : '500px'">
      <div v-if="sessionDetail" v-loading="detailLoading">
        <p>应结总额：<strong style="color:#f56c6c">{{ (sessionDetail.totalAmount / 100).toFixed(2) }} 元</strong></p>

        <el-descriptions title="订单明细" :column="1" border size="small" style="margin-top:10px">
          <template v-for="order in sessionDetail.orders" :key="order.id">
            <el-descriptions-item :label="order.orderNo">
              <span v-for="item in orderItemsMap[order.id]" :key="item.id" style="margin-right:10px">
                {{ item.dishName }} x{{ item.quantity }}
                <el-tag size="small" :type="itemStatusType(item.status)">{{ itemStatusText(item.status) }}</el-tag>
              </span>
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <el-form :model="checkoutForm" label-width="100px" style="margin-top:20px">
          <el-form-item label="实收金额(元)">
            <el-input-number v-model="checkoutForm.actualPaid" :min="0" :precision="2" :step="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="手机号(可选)">
            <el-input v-model="checkoutForm.phone" placeholder="填写手机号累加积分" />
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listTables } from '../../api/table'
import { current, detail, checkout } from '../../api/session'
import { getItems } from '../../api/order'
import { itemStatusText, itemStatusType } from '../../utils/orderStatus'

const loading = ref(false)
const detailLoading = ref(false)
const busyTables = ref([])
const showCheckout = ref(false)
const sessionDetail = ref(null)
const orderItemsMap = reactive({})
const checkoutForm = ref({ actualPaid: 0, phone: '' })
const currentTableId = ref(null)
const isMobile = ref(window.innerWidth <= 768)

const handleResize = () => { isMobile.value = window.innerWidth <= 768 }
onMounted(() => { loadTables(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })

const loadTables = async () => {
  loading.value = true
  try {
    const res = await listTables()
    busyTables.value = res.data.filter(t => t.status === 1)
  } catch (e) {
    ElMessage.error('加载桌台数据失败')
  } finally {
    loading.value = false
  }
}

const openSession = async (table) => {
  currentTableId.value = table.id
  detailLoading.value = true
  try {
    // 查找该桌当前 session
    const sessRes = await current(table.id)
    if (!sessRes.data) {
      ElMessage.warning('该桌无进行中会话')
      return
    }
    const detailRes = await detail(sessRes.data.id)
    const data = detailRes.data
    // detail API 返回 { session, orders, dishSummary, totalAmount }，无顶层 id
    sessionDetail.value = { ...data.session, orders: data.orders, totalAmount: data.totalAmount }

    // 清空旧的 items 映射
    Object.keys(orderItemsMap).forEach(k => delete orderItemsMap[k])

    // 并行加载每个订单的 items
    await Promise.all(sessionDetail.value.orders.map(async (order) => {
      const itemRes = await getItems(order.id)
      orderItemsMap[order.id] = itemRes.data
    }))

    checkoutForm.value.actualPaid = sessionDetail.value.totalAmount / 100
    checkoutForm.value.phone = ''
    showCheckout.value = true
  } catch (e) {
    ElMessage.error('加载会话详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleCheckout = async () => {
  loading.value = true
  try {
    await checkout(sessionDetail.value.id, checkoutForm.value)
    ElMessage.success('结账成功')
    showCheckout.value = false
    loadTables()
  } catch (e) {
    ElMessage.error('结账失败')
  } finally {
    loading.value = false
  }
}

</script>
