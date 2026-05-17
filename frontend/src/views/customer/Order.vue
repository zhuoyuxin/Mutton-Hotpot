<template>
  <div class="customer-page">
    <div class="category-tabs">
      <span
        v-for="cat in categories"
        :key="cat.id"
        :class="['tab-item', { active: activeCategory === cat.id }]"
        @click="activeCategory = cat.id"
      >{{ cat.name }}</span>
    </div>

    <div class="dish-list" v-loading="loading">
      <div v-for="dish in filteredDishes" :key="dish.id" class="dish-card">
        <img
          v-if="dish.image"
          :src="dish.image"
          class="dish-img"
          loading="lazy"
          :alt="dish.name"
          @error="$event.target.style.display='none'"
        />
        <div class="dish-info">
          <div>
            <div class="dish-name-row">
              <div class="dish-name">{{ dish.name }}</div>
              <span v-if="supportsHalf(dish)" class="half-badge">支持半份</span>
            </div>
            <div class="dish-desc">{{ dish.description || '现点现做，可在下单后备注口味。' }}</div>
          </div>

          <div class="portion-list">
            <div class="portion-row">
              <div class="portion-main">
                <span class="portion-label">整份</span>
                <span class="dish-price">&yen;{{ formatPrice(dish.price) }}</span>
              </div>
              <div class="dish-qty">
                <el-button
                  v-if="getCartQty(dish.id, PORTION_FULL)"
                  size="small"
                  circle
                  @click="changeQty(dish, PORTION_FULL, -1)"
                >-</el-button>
                <span v-if="getCartQty(dish.id, PORTION_FULL)" class="qty-num">{{ getCartQty(dish.id, PORTION_FULL) }}</span>
                <el-button
                  size="small"
                  circle
                  type="danger"
                  :disabled="getDishSelectedQty(dish.id) >= Number(dish.stock || 0)"
                  @click="changeQty(dish, PORTION_FULL, 1)"
                >+</el-button>
              </div>
            </div>

            <div v-if="supportsHalf(dish)" class="portion-row portion-row-half">
              <div class="portion-main">
                <span class="portion-label">半份</span>
                <span class="dish-price">&yen;{{ formatPrice(dish.halfPrice) }}</span>
              </div>
              <div class="dish-qty">
                <el-button
                  v-if="getCartQty(dish.id, PORTION_HALF)"
                  size="small"
                  circle
                  @click="changeQty(dish, PORTION_HALF, -1)"
                >-</el-button>
                <span v-if="getCartQty(dish.id, PORTION_HALF)" class="qty-num">{{ getCartQty(dish.id, PORTION_HALF) }}</span>
                <el-button
                  size="small"
                  circle
                  type="danger"
                  :disabled="getDishSelectedQty(dish.id) >= Number(dish.stock || 0)"
                  @click="changeQty(dish, PORTION_HALF, 1)"
                >+</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="cart-bar">
      <div class="cart-info" @click="showCartDetail = !showCartDetail">
        <el-badge :value="totalCount" :hidden="totalCount === 0" type="danger">
          <el-icon size="28"><ShoppingCart /></el-icon>
        </el-badge>
        <span class="cart-total">&yen;{{ formatPrice(totalPrice) }}</span>
      </div>
      <el-button type="danger" size="large" :disabled="totalCount === 0" @click="handleSubmit" :loading="submitting">
        提交订单
      </el-button>
    </div>

    <el-drawer v-model="showCartDetail" title="购物车" direction="btt" size="52%">
      <div v-for="item in cartItems" :key="item.cartKey" class="cart-item">
        <div class="cart-item-main">
          <span>{{ item.displayName }}</span>
          <span class="cart-item-price">&yen;{{ formatPrice(item.price * item.qty) }}</span>
        </div>
        <div class="dish-qty">
          <el-button size="small" circle @click="changeQty(item.dish, item.portionType, -1)">-</el-button>
          <span class="qty-num">{{ item.qty }}</span>
          <el-button
            size="small"
            circle
            :disabled="getDishSelectedQty(item.dishId) >= Number(item.dish.stock || 0)"
            @click="changeQty(item.dish, item.portionType, 1)"
          >+</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { customerList } from '../../api/dish'
import { customerCreate } from '../../api/order'
import { formatPrice } from '../../utils/format'
import {
  PORTION_FULL,
  PORTION_HALF,
  buildDishDisplayName,
  createCartKey,
  getPortionPrice,
  normalizePortionType,
  parseCartKey,
  supportsHalfPortion
} from '../../utils/portion'

const route = useRoute()
const router = useRouter()
const tableId = computed(() => route.params.tableId)

const categories = ref([{ id: 0, name: '全部' }])
const dishes = ref([])
const activeCategory = ref(0)
const cart = reactive({})
const submitting = ref(false)
const showCartDetail = ref(false)
const loading = ref(false)

const filteredDishes = computed(() => {
  if (activeCategory.value === 0) {
    return dishes.value
  }
  return dishes.value.filter((dish) => dish.categoryId === activeCategory.value)
})

const cartItems = computed(() => {
  return Object.entries(cart)
    .filter(([, qty]) => Number(qty) > 0)
    .map(([cartKey, qty]) => {
      const parsed = parseCartKey(cartKey)
      const dish = dishes.value.find((item) => item.id === parsed.dishId)
      if (!dish) {
        return null
      }
      return {
        cartKey,
        dishId: dish.id,
        dish,
        portionType: parsed.portionType,
        displayName: buildDishDisplayName(dish.name, parsed.portionType),
        price: getPortionPrice(dish, parsed.portionType),
        qty: Number(qty)
      }
    })
    .filter(Boolean)
})

const totalCount = computed(() => cartItems.value.reduce((sum, item) => sum + item.qty, 0))
const totalPrice = computed(() => cartItems.value.reduce((sum, item) => sum + item.price * item.qty, 0))

const supportsHalf = (dish) => supportsHalfPortion(dish)

const resetCart = () => {
  Object.keys(cart).forEach((key) => delete cart[key])
}

const getCartQty = (dishId, portionType) => Number(cart[createCartKey(dishId, portionType)] || 0)

const getDishSelectedQty = (dishId) => Object.entries(cart)
  .filter(([cartKey]) => parseCartKey(cartKey).dishId === Number(dishId))
  .reduce((sum, [, qty]) => sum + Number(qty || 0), 0)

const loadCart = (targetTableId) => {
  resetCart()
  const savedCart = localStorage.getItem('cart_' + targetTableId)
  if (!savedCart) {
    return
  }
  try {
    const parsedCart = JSON.parse(savedCart)
    Object.entries(parsedCart || {}).forEach(([key, qty]) => {
      const parsed = parseCartKey(key)
      if (parsed.dishId > 0 && Number(qty) > 0) {
        cart[createCartKey(parsed.dishId, parsed.portionType)] = Number(qty)
      }
    })
  } catch (error) {
    localStorage.removeItem('cart_' + targetTableId)
  }
}

const changeQty = (dish, portionType, delta) => {
  if (!dish) {
    return
  }

  const cartKey = createCartKey(dish.id, portionType)
  const nextQty = getCartQty(dish.id, portionType) + delta
  if (nextQty < 0) {
    return
  }
  if (delta > 0 && getDishSelectedQty(dish.id) >= Number(dish.stock || 0)) {
    ElMessage.warning('已达到当前库存上限')
    return
  }

  if (nextQty === 0) {
    delete cart[cartKey]
  } else {
    cart[cartKey] = nextQty
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await customerList()
    dishes.value = (res.data.dishes || res.data || []).filter((dish) => dish.status === 1)
    const cats = res.data.categories || []
    categories.value = [{ id: 0, name: '全部' }, ...cats]
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const items = cartItems.value.map((item) => ({
    dishId: item.dishId,
    quantity: item.qty,
    portionType: normalizePortionType(item.portionType)
  }))
  if (items.length === 0) {
    return
  }

  submitting.value = true
  try {
    const phone = localStorage.getItem('customerPhone') || null
    await customerCreate({
      tableId: Number(tableId.value),
      items,
      phone,
      remark: ''
    })
    ElMessage.success('下单成功')
    resetCart()
    localStorage.removeItem('cart_' + tableId.value)
    router.push('/c/status/' + tableId.value)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadCart(tableId.value)
})

watch(cart, (value) => {
  localStorage.setItem('cart_' + tableId.value, JSON.stringify(value))
}, { deep: true })

watch(tableId, (newTableId, oldTableId) => {
  if (newTableId === oldTableId) {
    return
  }
  activeCategory.value = 0
  showCartDetail.value = false
  loadCart(newTableId)
})
</script>

<style scoped>
.customer-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 70px;
}

.category-tabs {
  display: flex;
  overflow-x: auto;
  background: #fff;
  padding: calc(10px + var(--safe-top, 0px)) 10px 10px;
  position: sticky;
  top: 0;
  z-index: 10;
  gap: 10px;
}

.tab-item {
  white-space: nowrap;
  padding: 10px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.tab-item.active {
  background: #f56c6c;
  color: #fff;
}

.dish-list {
  padding: 10px;
  padding-bottom: calc(70px + env(safe-area-inset-bottom));
}

.dish-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 10px;
  overflow: hidden;
}

.dish-img {
  width: 108px;
  height: 128px;
  object-fit: cover;
}

.dish-info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
}

.dish-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dish-name {
  font-size: 16px;
  font-weight: 600;
}

.half-badge {
  font-size: 12px;
  color: #d35400;
  background: #fff2e8;
  border-radius: 999px;
  padding: 2px 8px;
}

.dish-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.portion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.portion-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #faf7f3;
}

.portion-row-half {
  background: #fff7ef;
}

.portion-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.portion-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.dish-price {
  color: #f56c6c;
  font-size: 15px;
  font-weight: 600;
}

.dish-qty {
  display: flex;
  align-items: center;
  gap: 6px;
}

.qty-num {
  min-width: 18px;
  text-align: center;
}

.cart-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  min-height: 60px;
  background: #333;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
  z-index: 100;
}

.cart-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  cursor: pointer;
}

.cart-total {
  font-size: 18px;
  font-weight: 500;
  color: #fff;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.cart-item-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cart-item-price {
  color: #f56c6c;
  font-size: 13px;
}
</style>
