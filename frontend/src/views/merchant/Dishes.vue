<template>
  <div v-loading="loading">
    <!-- 分类管理 -->
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div style="display:flex; justify-content:space-between">
              <span>分类管理</span>
              <el-button size="small" @click="showAddCategory = true">新增分类</el-button>
            </div>
          </template>
          <el-table :data="categories" size="small">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" text @click="editCategory(row)">编辑</el-button>
                <el-button size="small" text type="danger" @click="handleDeleteCategory(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && categories.length === 0" description="暂无分类" />
        </el-card>
      </el-col>

      <!-- 新增/编辑分类弹窗 -->
      <el-dialog v-model="showAddCategory" :title="editingCategory ? '编辑分类' : '新增分类'" :width="isMobile ? '92vw' : '300px'">
        <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules">
          <el-form-item label="分类名" prop="name">
            <el-input v-model="categoryForm.name" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="categoryForm.sortOrder" :min="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddCategory = false">取消</el-button>
          <el-button type="primary" @click="handleSaveCategory">确定</el-button>
        </template>
      </el-dialog>
    </el-row>

    <!-- 菜品列表 -->
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>菜品管理</span>
          <el-button type="primary" @click="openDishForm(null)">新增菜品</el-button>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="dishes">
          <el-table-column prop="name" label="名称" />
          <el-table-column label="价格(元)" width="100">
            <template #default="{ row }">{{ (row.price / 100).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" text @click="openDishForm(row)">编辑</el-button>
              <el-button size="small" text @click="handleToggle(row.id)">
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
              <el-button size="small" text @click="handleEditStock(row)">库存</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!loading && dishes.length === 0" description="暂无菜品" />
    </el-card>

    <!-- 库存修改弹窗 -->
    <el-dialog v-model="showStockDialog" title="修改库存" :width="isMobile ? '92vw' : '300px'">
      <el-input-number v-model="stockForm.stock" :min="0" style="width:100%" />
      <template #footer>
        <el-button @click="showStockDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveStock">确定</el-button>
      </template>
    </el-dialog>

    <DishFormDialog
      v-model:visible="showDishForm"
      :dish="editingDish"
      :categories="categories"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList as listDishes, toggleDish, updateStock } from '../../api/dish'
import { list as listCategories, add as addCategory, update as updateCategory, remove as removeCategory } from '../../api/category'
import DishFormDialog from '../../components/DishFormDialog.vue'

const loading = ref(false)
const categories = ref([])
const isMobile = ref(window.innerWidth <= 768)
const dishes = ref([])
const showAddCategory = ref(false)
const editingCategory = ref(null)
const categoryForm = ref({ name: '', sortOrder: 0 })
const categoryFormRef = ref(null)
const categoryRules = { name: [{ required: true, message: '请输入分类名', trigger: 'blur' }] }
const showDishForm = ref(false)
const editingDish = ref(null)
const showStockDialog = ref(false)
const stockForm = ref({ id: null, stock: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const [cRes, dRes] = await Promise.all([listCategories(), listDishes()])
    categories.value = cRes.data
    dishes.value = dRes.data
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const editCategory = (row) => {
  editingCategory.value = row
  categoryForm.value = { name: row.name, sortOrder: row.sortOrder }
  showAddCategory.value = true
}

const handleSaveCategory = async () => {
  if (!categoryFormRef.value) return
  await categoryFormRef.value.validate()
  if (editingCategory.value) {
    await updateCategory({ id: editingCategory.value.id, ...categoryForm.value })
  } else {
    await addCategory(categoryForm.value)
  }
  ElMessage.success('保存成功')
  showAddCategory.value = false
  editingCategory.value = null
  categoryForm.value = { name: '', sortOrder: 0 }
  loadData()
}

const handleDeleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该分类？', '提示')
    await removeCategory(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* 取消 */ }
}

const openDishForm = (dish) => {
  editingDish.value = dish
  showDishForm.value = true
}

const handleToggle = async (id) => {
  try {
    await toggleDish(id)
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleEditStock = (row) => {
  stockForm.value = { id: row.id, stock: row.stock }
  showStockDialog.value = true
}

const handleSaveStock = async () => {
  try {
    await updateStock(stockForm.value)
    showStockDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('修改库存失败')
  }
}

const handleResize = () => { isMobile.value = window.innerWidth <= 768 }
onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })
</script>

<style scoped>
.table-scroll {
  overflow-x: auto;
}
</style>
