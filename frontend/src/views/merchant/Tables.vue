<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>桌台管理</span>
          <el-button type="primary" @click="openForm(null)">新增桌台</el-button>
        </div>
      </template>
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
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showForm" :title="editing ? '编辑桌台' : '新增桌台'" width="400px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="桌号">
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
    <el-dialog v-model="showQRDialog" title="桌台二维码" width="350px">
      <div style="text-align:center">
        <p>{{ currentTable?.name }}</p>
        <img v-if="qrImage" :src="qrImage" style="width:250px" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { list, add, update, remove, qrcode } from '../../api/table'

const tables = ref([])
const showForm = ref(false)
const editing = ref(null)
const form = ref({ name: '', area: '大厅' })
const showQRDialog = ref(false)
const qrImage = ref('')
const currentTable = ref(null)

const loadData = async () => {
  const res = await list()
  tables.value = res.data
}

const openForm = (row) => {
  editing.value = row
  form.value = row ? { name: row.name, area: row.area } : { name: '', area: '大厅' }
  showForm.value = true
}

const handleSave = async () => {
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

onMounted(loadData)
</script>
