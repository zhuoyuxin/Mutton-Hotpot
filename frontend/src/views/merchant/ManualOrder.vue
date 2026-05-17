<template>
  <div v-loading="loading" class="manual-order-page" :class="{ 'mobile-has-summary': isMobile && selectedItems.length > 0 }">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>手动下单</span>
          <span class="header-tip">支持同一道菜分别选择整份和半份，库存按总份数共同限制。</span>
        </div>
      </template>

      <el-form :model="form" label-width="80px" class="base-form">
        <el-form-item label="桌台">
          <el-select
            v-model="form.tableId"
            placeholder="选择桌台（散客留空）"
            clearable
            :style="fieldStyle"
          >
            <el-option
              v-for="table in tables"
              :key="table.id"
              :label="`${table.name} (${table.area})`"
              :value="table.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="form.phone"
            placeholder="顾客手机号（可选）"
            :style="fieldStyle"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="订单备注" :style="fieldStyle" />
        </el-form-item>
      </el-form>

      <el-divider>选择菜品</el-divider>

      <div class="dish-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索菜品名称"
          clearable
          :style="isMobile ? 'width:100%' : 'width:280px'"
        />
        <div class="dish-toolbar-summary">
          <span>已选 {{ totalCount }} 份</span>
          <span>合计 {{ formatPrice(totalPrice) }} 元</span>
        </div>
      </div>

      <div class="category-tabs">
        <button
          type="button"
          class="category-tab"
          :class="{ active: activeCategoryId === 0 }"
          @click="activeCategoryId = 0"
        >
          全部
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="category-tab"
          :class="{ active: activeCategoryId === category.id }"
          @click="activeCategoryId = category.id"
        >
          {{ category.name }}
        </button>
      </div>

      <el-row :gutter="12">
        <el-col
          v-for="dish in filteredDishes"
          :key="dish.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          style="margin-bottom:12px"
        >
          <el-card shadow="hover" body-style="padding:14px" class="dish-card">
            <div class="dish-card-top">
              <div>
                <div class="dish-name">{{ dish.name }}</div>
                <div class="dish-desc">{{ dish.description || '可按口味备注，后厨按单制作。' }}</div>
              </div>
              <el-tag size="small" type="info">{{ categoryNameById[dish.categoryId] || '未分类' }}</el-tag>
            </div>

            <div class="dish-meta">
              <span class="dish-stock">库存 {{ dish.stock }}</span>
              <span v-if="supportsHalf(dish)" class="half-tip">支持半份</span>
            </div>

            <div class="portion-list">
              <div class="portion-row">
                <div class="portion-copy">
                  <span class="portion-label">整份</span>
                  <span class="portion-price">{{ formatPrice(dish.price) }} 元</span>
                </div>
                <el-input-number
                  :model-value="getCartQty(dish.id, PORTION_FULL)"
                  :min="0"
                  :max="getMaxQtyForPortion(dish, PORTION_FULL)"
                  size="small"
                  controls-position="right"
                  @update:model-value="(value) => updateCartQty(dish, PORTION_FULL, value)"
                />
              </div>

              <div v-if="supportsHalf(dish)" class="portion-row portion-row-half">
                <div class="portion-copy">
                  <span class="portion-label">半份</span>
                  <span class="portion-price">{{ formatPrice(dish.halfPrice) }} 元</span>
                </div>
                <el-input-number
                  :model-value="getCartQty(dish.id, PORTION_HALF)"
                  :min="0"
                  :max="getMaxQtyForPortion(dish, PORTION_HALF)"
                  size="small"
                  controls-position="right"
                  @update:model-value="(value) => updateCartQty(dish, PORTION_HALF, value)"
                />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty
        v-if="!loading && filteredDishes.length === 0"
        description="当前筛选条件下暂无菜品"
      />

      <template v-if="selectedItems.length > 0 && !isMobile">
        <el-divider>订单汇总</el-divider>
        <div class="table-scroll">
          <el-table :data="selectedItems" size="small" style="margin-bottom:16px">
            <el-table-column prop="displayName" label="菜品" min-width="180" />
            <el-table-column label="分类" width="120">
              <template #default="{ row }">{{ categoryNameById[row.categoryId] || '未分类' }}</template>
            </el-table-column>
            <el-table-column prop="portionLabel" label="份量" width="90" />
            <el-table-column label="单价(元)" width="100">
              <template #default="{ row }">{{ formatPrice(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" width="80" />
            <el-table-column label="小计(元)" width="100">
              <template #default="{ row }">{{ formatPrice(row.price * row.qty) }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div class="total-bar">
          合计：<span class="total-price">{{ formatPrice(totalPrice) }} 元</span>
        </div>
      </template>

      <el-divider v-if="!isMobile" />
      <el-button v-if="!isMobile" type="primary" @click="handleSubmit" :loading="submitting">提交订单</el-button>
    </el-card>

    <div v-if="isMobile && selectedItems.length > 0" class="mobile-summary-bar">
      <div class="mobile-summary-info" @click="showSummaryDrawer = true">
        <div class="mobile-summary-main">{{ totalCount }} 份 · {{ formatPrice(totalPrice) }} 元</div>
        <div class="mobile-summary-sub">点击查看订单汇总</div>
      </div>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
    </div>

    <el-drawer
      v-model="showSummaryDrawer"
      title="订单汇总"
      direction="btt"
      size="56%"
      :with-header="true"
    >
      <div class="drawer-content">
        <div class="table-scroll">
          <el-table :data="selectedItems" size="small" style="margin-bottom:16px">
            <el-table-column prop="displayName" label="菜品" min-width="160" />
            <el-table-column prop="portionLabel" label="份量" width="90" />
            <el-table-column prop="qty" label="数量" width="70" />
            <el-table-column label="小计(元)" width="100">
              <template #default="{ row }">{{ formatPrice(row.price * row.qty) }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div class="total-bar">
          合计：<span class="total-price">{{ formatPrice(totalPrice) }} 元</span>
        </div>
        <el-button type="primary" style="width:100%" @click="handleSubmit" :loading="submitting">
          提交订单
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listCategories } from '../../api/category'
import { merchantList as listDishes } from '../../api/dish'
import { merchantCreate } from '../../api/order'
import { list as listTables } from '../../api/table'
import { formatPrice } from '../../utils/format'
import {
  PORTION_FULL,
  PORTION_HALF,
  buildDishDisplayName,
  createCartKey,
  getPortionLabel,
  getPortionPrice,
  normalizePortionType,
  parseCartKey,
  supportsHalfPortion
} from '../../utils/portion'

const loading = ref(false)
const submitting = ref(false)
const tables = ref([])
const categories = ref([])
const dishes = ref([])
const keyword = ref('')
const activeCategoryId = ref(0)
const cart = reactive({})
const form = ref({ tableId: null, phone: '', remark: '' })
const isMobile = ref(window.innerWidth <= 768)
const showSummaryDrawer = ref(false)

const fieldStyle = computed(() => (isMobile.value ? 'width:100%' : 'width:320px'))

const categoryNameById = computed(() => {
  const map = {}
  categories.value.forEach((category) => {
    map[category.id] = category.name
  })
  return map
})

const filteredDishes = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return dishes.value.filter((dish) => {
    const matchCategory = activeCategoryId.value === 0 || dish.categoryId === activeCategoryId.value
    const matchKeyword = !normalizedKeyword || dish.name.toLowerCase().includes(normalizedKeyword)
    return matchCategory && matchKeyword
  })
})

const selectedItems = computed(() => {
  return Object.entries(cart)
    .filter(([, qty]) => Number(qty) > 0)
    .map(([cartKey, qty]) => {
      const parsed = parseCartKey(cartKey)
      const dish = dishes.value.find((item) => item.id === parsed.dishId)
      if (!dish) {
        return null
      }
      const price = getPortionPrice(dish, parsed.portionType)
      return {
        cartKey,
        dishId: dish.id,
        categoryId: dish.categoryId,
        name: dish.name,
        displayName: buildDishDisplayName(dish.name, parsed.portionType),
        portionType: parsed.portionType,
        portionLabel: getPortionLabel(parsed.portionType),
        price,
        qty: Number(qty)
      }
    })
    .filter(Boolean)
})

const totalCount = computed(() => selectedItems.value.reduce((sum, dish) => sum + dish.qty, 0))
const totalPrice = computed(() => selectedItems.value.reduce((sum, dish) => sum + dish.price * dish.qty, 0))

const supportsHalf = (dish) => supportsHalfPortion(dish)

const getCartQty = (dishId, portionType) => Number(cart[createCartKey(dishId, portionType)] || 0)

const getDishSelectedQty = (dishId) => Object.entries(cart)
  .filter(([cartKey]) => parseCartKey(cartKey).dishId === Number(dishId))
  .reduce((sum, [, qty]) => sum + Number(qty || 0), 0)

const getMaxQtyForPortion = (dish, portionType) => {
  const currentQty = getCartQty(dish.id, portionType)
  const otherQty = getDishSelectedQty(dish.id) - currentQty
  return Math.max(Number(dish.stock || 0) - otherQty, 0)
}

const updateCartQty = (dish, portionType, value) => {
  const cartKey = createCartKey(dish.id, portionType)
  const maxQty = getMaxQtyForPortion(dish, portionType)
  const nextQty = Math.max(0, Math.min(Number(value || 0), maxQty))
  if (nextQty === 0) {
    delete cart[cartKey]
  } else {
    cart[cartKey] = nextQty
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [tableRes, dishRes, categoryRes] = await Promise.all([
      listTables(),
      listDishes(),
      listCategories()
    ])
    tables.value = tableRes.data || []
    dishes.value = (dishRes.data || []).filter((dish) => dish.status === 1)
    categories.value = categoryRes.data || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const resetCart = () => {
  Object.keys(cart).forEach((key) => delete cart[key])
}

const handleSubmit = async () => {
  const items = selectedItems.value.map((item) => ({
    dishId: item.dishId,
    quantity: item.qty,
    portionType: normalizePortionType(item.portionType)
  }))

  if (items.length === 0) {
    ElMessage.warning('请选择菜品')
    return
  }

  submitting.value = true
  try {
    await merchantCreate({
      tableId: form.value.tableId || null,
      items,
      phone: form.value.phone || null,
      remark: form.value.remark || null
    })
    ElMessage.success('下单成功')
    resetCart()
    form.value.remark = ''
    showSummaryDrawer.value = false
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    showSummaryDrawer.value = false
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.manual-order-page {
  position: relative;
}

.manual-order-page.mobile-has-summary {
  padding-bottom: calc(92px + env(safe-area-inset-bottom));
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.header-tip {
  color: #909399;
  font-size: 12px;
}

.base-form {
  margin-bottom: 8px;
}

.dish-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.dish-toolbar-summary {
  display: flex;
  gap: 12px;
  color: #606266;
  font-size: 13px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 12px;
}

.category-tab {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.category-tab.active {
  border-color: #f56c6c;
  background: #f56c6c;
  color: #fff;
}

.dish-card {
  height: 100%;
}

.dish-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.dish-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.dish-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.dish-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
}

.dish-stock {
  color: #909399;
}

.half-tip {
  color: #e67e22;
  font-weight: 600;
}

.portion-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.portion-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #faf7f3;
}

.portion-row-half {
  background: #fff7ef;
}

.portion-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.portion-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.portion-price {
  font-size: 13px;
  color: #f56c6c;
  font-weight: 600;
}

.total-bar {
  text-align: right;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.total-price {
  color: #f56c6c;
}

.table-scroll {
  overflow-x: auto;
}

.mobile-summary-bar {
  position: fixed;
  left: 10px;
  right: 10px;
  bottom: calc(10px + env(safe-area-inset-bottom));
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(10px);
}

.mobile-summary-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}

.mobile-summary-main {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.mobile-summary-sub {
  font-size: 12px;
  color: #909399;
}

.drawer-content {
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
}

@media (max-width: 768px) {
  .dish-toolbar-summary {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
