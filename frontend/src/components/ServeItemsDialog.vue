<template>
  <el-dialog
    v-model="dialogVisible"
    title="上菜"
    :width="isMobile ? '92vw' : '640px'"
    @closed="resetState"
  >
    <div v-if="pendingItems.length === 0">
      <el-empty description="暂无待上菜菜品" :image-size="60" />
    </div>
    <div v-else class="table-scroll">
      <el-table :data="pendingItems" size="small">
        <el-table-column prop="dishName" label="菜品" />
        <el-table-column prop="quantity" label="待上份数" width="100" />
        <el-table-column label="本次上菜" width="190">
          <template #default="{ row }">
            <el-input-number
              v-model="serveQuantities[row.id]"
              :min="1"
              :max="row.quantity"
              :step="1"
              :precision="0"
              step-strictly
              controls-position="right"
              style="width: 140px"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              type="success"
              size="small"
              :loading="servingItemId === row.id"
              @click="handleServe(row)"
            >
              上菜
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { serveItem } from '../api/order'

const props = defineProps({
  visible: { type: Boolean, default: false },
  order: { type: Object, default: null },
  filterItemId: { type: Number, default: null }
})

const emit = defineEmits(['update:visible', 'served'])

const isMobile = ref(window.innerWidth <= 768)
const serveQuantities = ref({})
const servingItemId = ref(null)

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const pendingItems = computed(() => {
  const items = props.order?.items || []
  return items.filter((item) => item.status === 1 && (!props.filterItemId || item.id === props.filterItemId))
})

const initServeQuantities = () => {
  const next = {}
  pendingItems.value.forEach((item) => {
    next[item.id] = item.quantity
  })
  serveQuantities.value = next
}

const resetState = () => {
  serveQuantities.value = {}
  servingItemId.value = null
}

const handleServe = async (item) => {
  const quantity = Number(serveQuantities.value[item.id])
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > item.quantity) {
    ElMessage.error('上菜份数不合法')
    return
  }

  servingItemId.value = item.id
  try {
    await serveItem(item.id, {
      quantity,
      expectedQuantity: item.quantity
    })
    ElMessage.success(quantity === item.quantity ? '上菜成功' : '已按指定份数上菜')
    dialogVisible.value = false
    emit('served')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '上菜操作失败')
  } finally {
    servingItemId.value = null
  }
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

watch(
  () => [props.visible, props.order, props.filterItemId],
  () => {
    if (props.visible) {
      initServeQuantities()
    }
  },
  { deep: true }
)

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.table-scroll {
  overflow-x: auto;
}
</style>
