<template>
  <div class="customer-page">
    <!-- 分类标签 -->
    <div class="category-tabs">
      <span
        v-for="cat in categories" :key="cat.id"
        :class="['tab-item', { active: activeCategory === cat.id }]"
        @click="activeCategory = cat.id"
      >{{ cat.name }}</span>
    </div>

    <!-- 菜品列表 -->
    <div class="dish-list" v-loading="loading">
      <div v-for="dish in filteredDishes" :key="dish.id" class="dish-card">
        <img v-if="dish.image" :src="dish.image" class="dish-img" loading="lazy" :alt="dish.name" @error="$event.target.style.display='none'" />
        <div class="dish-info">
          <div class="dish-name">{{ dish.name }}</div>
          <div class="dish-desc">{{ dish.description }}</div>
          <div class="dish-bottom">
            <span class="dish-price">&yen;{{ formatPrice(dish.price) }}</span>
            <div class="dish-qty">
              <el-button v-if="cart[dish.id]" size="small" circle @click="changeQty(dish.id, -1)">-</el-button>
              <span v-if="cart[dish.id]" class="qty-num">{{ cart[dish.id] }}</span>
              <el-button size="small" circle type="danger" @click="changeQty(dish.id, 1)">+</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部购物车 -->
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

    <!-- 购物车详情弹窗 -->
    <el-drawer v-model="showCartDetail" title="购物车" direction="btt" size="50%">
      <div v-for="item in cartItems" :key="item.dishId" style="display:flex; justify-content:space-between; padding:10px 0; border-bottom:1px solid #eee">
        <span>{{ item.name }}</span>
        <div>
          <el-button size="small" circle @click="changeQty(item.dishId, -1)">-</el-button>
          <span style="margin:0 8px">{{ item.qty }}</span>
          <el-button size="small" circle @click="changeQty(item.dishId, 1)">+</el-button>
          <span style="margin-left:10px; color:#f56c6c">&yen;{{ formatPrice(item.price * item.qty) }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { customerList } from '../../api/dish'
import { customerCreate } from '../../api/order'
import { formatPrice } from '../../utils/format'

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
  if (activeCategory.value === 0) return dishes.value
  return dishes.value.filter(d => d.categoryId === activeCategory.value)
})

const cartItems = computed(() => {
  return Object.entries(cart)
    .filter(([, qty]) => qty > 0)
    .map(([dishId, qty]) => {
      const dish = dishes.value.find(d => d.id === Number(dishId))
      return { dishId: Number(dishId), name: dish?.name, price: dish?.price || 0, qty }
    })
})

const totalCount = computed(() => Object.values(cart).reduce((s, q) => s + q, 0))
const totalPrice = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.qty, 0))

const changeQty = (dishId, delta) => {
  const newVal = (cart[dishId] || 0) + delta
  if (newVal < 0) return
  if (newVal === 0) {
    delete cart[dishId]
  } else {
    cart[dishId] = newVal
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await customerList()
    dishes.value = res.data.dishes || res.data
    // 使用后端返回的分类数据
    const cats = res.data.categories || []
    categories.value = [{ id: 0, name: '全部' }, ...cats]
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const items = cartItems.value.map(i => ({ dishId: i.dishId, quantity: i.qty }))
  if (items.length === 0) return

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
    Object.keys(cart).forEach(k => delete cart[k])
    localStorage.removeItem('cart_' + tableId.value)
    router.push('/c/status/' + tableId.value)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  const savedCart = localStorage.getItem('cart_' + tableId.value)
  if (savedCart) {
    try { Object.assign(cart, JSON.parse(savedCart)) } catch (e) {}
  }
})

watch(cart, (val) => {
  localStorage.setItem('cart_' + tableId.value, JSON.stringify(val))
}, { deep: true })
</script>

<style scoped>
.customer-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 70px; }
.category-tabs {
  display: flex; overflow-x: auto; background: #fff; padding: calc(10px + var(--safe-top, 0px)) 10px 10px;
  position: sticky; top: 0; z-index: 10; gap: 10px;
}
.tab-item { white-space: nowrap; padding: 10px 16px; border-radius: 20px; font-size: 14px; color: #666; cursor: pointer; }
.tab-item.active { background: #f56c6c; color: #fff; }
.dish-list { padding: 10px; padding-bottom: calc(70px + env(safe-area-inset-bottom)); }
.dish-card { display: flex; background: #fff; border-radius: 8px; margin-bottom: 10px; overflow: hidden; }
.dish-img { width: 100px; height: 100px; object-fit: cover; }
.dish-info { flex: 1; padding: 10px; display: flex; flex-direction: column; justify-content: space-between; }
.dish-name { font-size: 16px; font-weight: 500; }
.dish-desc { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dish-bottom { display: flex; justify-content: space-between; align-items: center; }
.dish-price { color: #f56c6c; font-size: 16px; font-weight: 500; }
.dish-qty { display: flex; align-items: center; gap: 6px; }
.cart-bar {
  position: fixed; bottom: 0; left: 0; right: 0; min-height: 60px;
  background: #333; display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; padding-bottom: calc(10px + env(safe-area-inset-bottom));
  z-index: 100;
}
.cart-info { display: flex; align-items: center; gap: 10px; color: #fff; cursor: pointer; }
.cart-total { font-size: 18px; font-weight: 500; color: #fff; }
</style>
