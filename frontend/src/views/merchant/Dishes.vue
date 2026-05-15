<template>
  <div>
    <!-- 分类管理 -->
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="12">
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
        </el-card>
      </el-col>

      <!-- 新增/编辑分类弹窗 -->
      <el-dialog v-model="showAddCategory" :title="editingCategory ? '编辑分类' : '新增分类'" width="300px">
        <el-form>
          <el-form-item label="分类名">
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
    </el-card>

    <!-- 库存修改弹窗 -->
    <el-dialog v-model="showStockDialog" title="修改库存" width="300px">
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList as listDishes, toggleDish, updateStock } from '../../api/dish'
import { list as listCategories, add as addCategory, update as updateCategory, remove as removeCategory } from '../../api/category'
import DishFormDialog from '../../components/DishFormDialog.vue'

const categories = ref([])
const dishes = ref([])
const showAddCategory = ref(false)
const editingCategory = ref(null)
const categoryForm = ref({ name: '', sortOrder: 0 })
const showDishForm = ref(false)
const editingDish = ref(null)
const showStockDialog = ref(false)
const stockForm = ref({ id: null, stock: 0 })

const loadData = async () => {
  const [cRes, dRes] = await Promise.all([listCategories(), listDishes()])
  categories.value = cRes.data
  dishes.value = dRes.data
}

const editCategory = (row) => {
  editingCategory.value = row
  categoryForm.value = { name: row.name, sortOrder: row.sortOrder }
  showAddCategory.value = true
}

const handleSaveCategory = async () => {
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
  await toggleDish(id)
  loadData()
}

const handleEditStock = (row) => {
  stockForm.value = { id: row.id, stock: row.stock }
  showStockDialog.value = true
}

const handleSaveStock = async () => {
  await updateStock(stockForm.value)
  showStockDialog.value = false
  loadData()
}

onMounted(loadData)
</script>
