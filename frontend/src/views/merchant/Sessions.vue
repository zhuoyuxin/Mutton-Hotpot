<template>
  <div>
    <el-card>
      <template #header>
        <span>会话结账</span>
      </template>
      <el-row :gutter="16">
        <el-col v-for="table in busyTables" :key="table.id" :span="6" style="margin-bottom:16px">
          <el-card shadow="hover" @click="openSession(table)" style="cursor:pointer">
            <div style="text-align:center">
              <h3>{{ table.name }}</h3>
              <el-tag type="warning">{{ table.area }} - 使用中</el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结账弹窗 -->
    <el-dialog v-model="showCheckout" title="整桌结账" width="500px">
      <div v-if="sessionDetail">
        <p>应结总额：<strong style="color:#f56c6c">{{ (sessionDetail.totalAmount / 100).toFixed(2) }} 元</strong></p>

        <el-descriptions title="订单明细" :column="1" border size="small" style="margin-top:10px">
          <template v-for="order in sessionDetail.orders" :key="order.id">
            <el-descriptions-item :label="order.orderNo">
              <span v-for="item in order._items" :key="item.id" style="margin-right:10px">
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listTables } from '../../api/table'
import { current, detail, checkout } from '../../api/session'
import { getItems } from '../../api/order'

const busyTables = ref([])
const showCheckout = ref(false)
const sessionDetail = ref(null)
const checkoutForm = ref({ actualPaid: 0, phone: '' })
const loading = ref(false)
const currentTableId = ref(null)

const itemStatusText = (s) => ['待确认','待上菜','已上菜','库存不足','已退菜'][s] || ''
const itemStatusType = (s) => ['info','','success','warning','danger'][s] || ''

const loadTables = async () => {
  const res = await listTables()
  busyTables.value = res.data.filter(t => t.status === 1)
}

const openSession = async (table) => {
  currentTableId.value = table.id
  // 查找该桌当前 session
  const sessRes = await current(table.id)
  if (!sessRes.data) {
    ElMessage.warning('该桌无进行中会话')
    return
  }
  const detailRes = await detail(sessRes.data.id)
  sessionDetail.value = detailRes.data

  // 加载每个订单的 items
  for (const order of sessionDetail.value.orders) {
    const itemRes = await getItems(order.id)
    order._items = itemRes.data
  }

  checkoutForm.value.actualPaid = sessionDetail.value.totalAmount / 100
  checkoutForm.value.phone = ''
  showCheckout.value = true
}

const handleCheckout = async () => {
  loading.value = true
  try {
    const sessRes = await current(currentTableId.value)
    await checkout(sessRes.data.id, checkoutForm.value)
    ElMessage.success('结账成功')
    showCheckout.value = false
    loadTables()
  } finally {
    loading.value = false
  }
}

onMounted(loadTables)
</script>
