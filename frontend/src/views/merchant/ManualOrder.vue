<template>
  <div v-loading="loading">
    <el-card>
      <template #header><span>手动下单</span></template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="桌台">
          <el-select v-model="form.tableId" placeholder="选择桌台（散客留空）" clearable style="width:300px">
            <el-option v-for="t in tables" :key="t.id" :label="t.name + ' (' + t.area + ')'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="客户手机号（可选）" style="width:300px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" style="width:300px" />
        </el-form-item>
      </el-form>

      <!-- 菜品选择 -->
      <el-divider>选择菜品</el-divider>
      <el-row :gutter="12">
        <el-col v-for="dish in dishes" :key="dish.id" :span="6" style="margin-bottom:12px">
          <el-card shadow="hover" body-style="padding:10px">
            <div>{{ dish.name }}</div>
            <div style="color:#f56c6c; font-size:14px">{{ formatPrice(dish.price) }}元</div>
            <div style="margin-top:5px">
              <el-input-number v-model="cart[dish.id]" :min="0" :max="dish.stock" size="small" />
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && dishes.length === 0" description="暂无菜品" />

      <!-- 订单汇总 -->
      <template v-if="selectedItems.length > 0">
        <el-divider>订单汇总</el-divider>
        <el-table :data="selectedItems" size="small" style="margin-bottom:16px">
          <el-table-column prop="name" label="菜品" />
          <el-table-column label="单价(元)" width="100">
            <template #default="{ row }">{{ formatPrice(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="80" />
          <el-table-column label="小计(元)" width="100">
            <template #default="{ row }">{{ formatPrice(row.price * row.qty) }}</template>
          </el-table-column>
        </el-table>
        <div style="text-align:right; font-size:16px; font-weight:bold; margin-bottom:16px">
          合计：<span style="color:#f56c6c">{{ formatPrice(totalPrice) }} 元</span>
        </div>
      </template>

      <el-divider />
      <el-button type="primary" @click="handleSubmit" :loading="loading">提交订单</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listTables } from '../../api/table'
import { merchantList as listDishes } from '../../api/dish'
import { merchantCreate } from '../../api/order'
import { formatPrice } from '../../utils/format'

const loading = ref(false)
const tables = ref([])
const dishes = ref([])
const cart = reactive({})
const form = ref({ tableId: null, phone: '', remark: '' })

const selectedItems = computed(() => dishes.value.filter(d => cart[d.id] > 0).map(d => ({ ...d, qty: cart[d.id] })))
const totalPrice = computed(() => selectedItems.value.reduce((sum, d) => sum + d.price * d.qty, 0))

const loadData = async () => {
  loading.value = true
  try {
    const [tRes, dRes] = await Promise.all([listTables(), listDishes()])
    tables.value = tRes.data
    // 商户端 /api/m/dish/list 返回全部菜品数组，过滤仅展示上架菜品
    dishes.value = (dRes.data || []).filter(d => d.status === 1)
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const items = []
  for (const [dishId, qty] of Object.entries(cart)) {
    if (qty > 0) items.push({ dishId: Number(dishId), quantity: qty })
  }
  if (items.length === 0) {
    ElMessage.warning('请选择菜品')
    return
  }

  loading.value = true
  try {
    await merchantCreate({
      tableId: form.value.tableId || null,
      items,
      phone: form.value.phone || null,
      remark: form.value.remark || null
    })
    ElMessage.success('下单成功')
    // 清空购物车
    Object.keys(cart).forEach(k => delete cart[k])
    form.value.remark = ''
  } catch (e) {
    ElMessage.error('下单失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
