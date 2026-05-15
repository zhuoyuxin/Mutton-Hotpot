<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日订单" :value="data.todayOrders || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日营收(元)" :value="(data.todayRevenue || 0) / 100" :precision="2" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="空闲桌台" :value="data.freeTables || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="进行中会话" :value="data.activeSessions || 0" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getData } from '../../api/dashboard'

const data = ref({})
onMounted(async () => {
  const res = await getData()
  data.value = res.data
})
</script>
