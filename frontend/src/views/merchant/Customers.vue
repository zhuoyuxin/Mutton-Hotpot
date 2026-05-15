<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>客户管理</span>
          <el-input v-model="keyword" placeholder="搜索手机号/名称" style="width:200px" @keyup.enter="loadCustomers" clearable>
            <template #append>
              <el-button @click="loadCustomers">搜索</el-button>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="customers">
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="积分" width="100">
          <template #default="{ row }">{{ row.points }}</template>
        </el-table-column>
        <el-table-column label="累计消费(元)" width="120">
          <template #default="{ row }">{{ (row.totalSpent / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" text @click="openDetail(row)">详情</el-button>
            <el-button size="small" text @click="openPoints(row)">积分</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 客户详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="客户详情" width="500px">
      <template v-if="detailData">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="手机号">{{ detailData.customer?.phone }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detailData.customer?.name }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ detailData.customer?.points }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">{{ ((detailData.customer?.totalSpent || 0) / 100).toFixed(2) }} 元</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 15px 0 10px">积分明细</h4>
        <el-table :data="detailData.pointsRecords || []" size="small" max-height="300">
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="积分变动" width="100">
            <template #default="{ row }">
              <span :style="{ color: row.points > 0 ? '#67c23a' : '#f56c6c' }">
                {{ row.points > 0 ? '+' : '' }}{{ row.points }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160" />
        </el-table>
      </template>
    </el-dialog>

    <!-- 积分操作弹窗 -->
    <el-dialog v-model="showPointsDialog" title="手动调整积分" width="400px">
      <p>客户：{{ currentCustomer?.name }}（当前积分：{{ currentCustomer?.points }}）</p>
      <el-input-number v-model="pointsForm.points" style="width:100%; margin-top:10px" />
      <p style="color:#999; margin-top:5px">正数为增加，负数为扣减</p>
      <el-input v-model="pointsForm.remark" placeholder="备注原因" style="margin-top:10px" />
      <template #footer>
        <el-button @click="showPointsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSavePoints">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantList, merchantDetail, manualPoints } from '../../api/customer'

const customers = ref([])
const keyword = ref('')
const showPointsDialog = ref(false)
const showDetailDialog = ref(false)
const detailData = ref(null)
const currentCustomer = ref(null)
const pointsForm = ref({ customerId: null, points: 0, remark: '' })

const loadCustomers = async () => {
  const res = await merchantList({ keyword: keyword.value })
  customers.value = res.data
}

const openDetail = async (row) => {
  const res = await merchantDetail(row.id)
  detailData.value = res.data
  showDetailDialog.value = true
}

const openPoints = (row) => {
  currentCustomer.value = row
  pointsForm.value = { customerId: row.id, points: 0, remark: '' }
  showPointsDialog.value = true
}

const handleSavePoints = async () => {
  await manualPoints(pointsForm.value)
  ElMessage.success('积分调整成功')
  showPointsDialog.value = false
  loadCustomers()
}

onMounted(loadCustomers)
</script>
