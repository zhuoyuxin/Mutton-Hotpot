<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>桌台管理</span>
          <el-button type="primary" @click="openForm(null)">新增桌台</el-button>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="tables">
          <el-table-column prop="name" label="桌号" />
          <el-table-column prop="area" label="区域" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'warning'">
                {{ row.status === 0 ? '空闲' : '使用中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250">
            <template #default="{ row }">
              <el-button size="small" text @click="openForm(row)">编辑</el-button>
              <el-button size="small" text @click="showQR(row)">二维码</el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!loading && tables.length === 0" description="暂无桌台" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showForm" :title="editing ? '编辑桌台' : '新增桌台'" :width="isMobile ? '92vw' : '400px'">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="60px">
        <el-form-item label="桌号" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="form.area" style="width:100%">
            <el-option label="大厅" value="大厅" />
            <el-option label="包厢" value="包厢" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 二维码弹窗 -->
    <el-dialog v-model="showQRDialog" title="桌台二维码" :width="isMobile ? '92vw' : '350px'">
      <div style="text-align:center">
        <p>{{ currentTable?.name }}</p>
        <img v-if="qrImage" :src="qrImage" alt="二维码" :style="isMobile ? 'width:60vw;max-width:250px' : 'width:250px'" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { list, add, update, remove, qrcode } from '../../api/table'

const loading = ref(false)
const tables = ref([])
const showForm = ref(false)
const editing = ref(null)
const form = ref({ name: '', area: '大厅' })
const formRef = ref(null)
const formRules = { name: [{ required: true, message: '请输入桌号', trigger: 'blur' }] }
const showQRDialog = ref(false)
const qrImage = ref('')
const currentTable = ref(null)
const isMobile = ref(window.innerWidth <= 768)

const handleResize = () => { isMobile.value = window.innerWidth <= 768 }
onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })

const loadData = async () => {
  loading.value = true
  try {
    const res = await list()
    tables.value = res.data
  } catch (e) {
    ElMessage.error('加载桌台数据失败')
  } finally {
    loading.value = false
  }
}

const openForm = (row) => {
  editing.value = row
  form.value = row ? { name: row.name, area: row.area } : { name: '', area: '大厅' }
  showForm.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (editing.value) {
    await update({ id: editing.value.id, ...form.value })
  } else {
    await add(form.value)
  }
  showForm.value = false
  ElMessage.success('保存成功')
  loadData()
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该桌台？', '提示')
    await remove(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* 取消 */ }
}

const showQR = async (row) => {
  currentTable.value = row
  const res = await qrcode(row.id)
  qrImage.value = res.data.image
  showQRDialog.value = true
}
</script>

<style scoped>
.table-scroll {
  overflow-x: auto;
}
</style>
