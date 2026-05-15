<template>
  <div v-loading="loading">
    <!-- 日期筛选 -->
    <el-card style="margin-bottom:16px">
      <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期"
          value-format="YYYY-MM-DD" style="max-width:280px" />
        <el-button type="primary" @click="loadAll">查询</el-button>
      </div>
    </el-card>

    <!-- 营收趋势 -->
    <el-card style="margin-bottom:16px">
      <template #header><span>营收趋势</span></template>
      <div v-if="revenueData.length > 0">
        <div class="bar-chart">
          <div v-for="item in revenueData" :key="item.date" class="bar-item">
            <div class="bar-wrapper">
              <div class="bar" :style="{ height: barHeight(item.revenue, maxRevenue) + 'px' }"></div>
            </div>
            <div class="bar-label">{{ item.date.slice(5) }}</div>
            <div class="bar-value">{{ formatPrice(item.revenue) }}元</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无数据" :image-size="60" />
    </el-card>

    <!-- 热销菜品 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card style="margin-bottom:16px">
          <template #header><span>热销菜品 Top 10</span></template>
          <div class="table-scroll">
            <el-table :data="topDishesData" size="small">
              <el-table-column type="index" label="#" width="40" />
              <el-table-column prop="dishName" label="菜品" />
              <el-table-column label="销量" width="80">
                <template #default="{ row }">{{ row.quantity }}</template>
              </el-table-column>
              <el-table-column label="营收(元)" width="100">
                <template #default="{ row }">{{ formatPrice(row.revenue) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-if="topDishesData.length === 0" description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card style="margin-bottom:16px">
          <template #header><span>订单时段分布</span></template>
          <div v-if="hourlyData.length > 0" class="bar-chart">
            <div v-for="item in hourlyData" :key="item.hour" class="bar-item">
              <div class="bar-wrapper">
                <div class="bar bar-blue" :style="{ height: barHeight(item.count, maxHourly) + 'px' }"></div>
              </div>
              <div class="bar-label">{{ item.hour }}时</div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { revenueTrend, topDishes, hourlyDistribution } from '../../api/statistics'
import { formatPrice } from '../../utils/format'

const loading = ref(false)
const dateRange = ref(null)
const revenueData = ref([])
const topDishesData = ref([])
const hourlyData = ref([])

const maxRevenue = computed(() => Math.max(...revenueData.value.map(r => r.revenue), 1))
const maxHourly = computed(() => Math.max(...hourlyData.value.map(r => r.count), 1))

const barHeight = (value, max) => {
  return Math.max(2, (value / max) * 120)
}

const getParams = () => {
  const params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

const loadAll = async () => {
  loading.value = true
  const params = getParams()
  try {
    const [r1, r2, r3] = await Promise.all([
      revenueTrend(params),
      topDishes(params),
      hourlyDistribution(params)
    ])
    revenueData.value = r1.data
    topDishesData.value = r2.data
    hourlyData.value = r3.data
  } catch (e) {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  overflow-x: auto;
  padding: 10px 0;
  min-height: 180px;
}
.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 36px;
  flex-shrink: 0;
}
.bar-wrapper {
  height: 120px;
  display: flex;
  align-items: flex-end;
}
.bar {
  width: 24px;
  background: linear-gradient(to top, #409eff, #79bbff);
  border-radius: 3px 3px 0 0;
  transition: height 0.3s;
}
.bar-blue {
  background: linear-gradient(to top, #67c23a, #95d475);
}
.bar-label {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  white-space: nowrap;
}
.bar-value {
  font-size: 10px;
  color: #666;
  margin-bottom: 2px;
  white-space: nowrap;
}
.table-scroll {
  overflow-x: auto;
}
</style>
