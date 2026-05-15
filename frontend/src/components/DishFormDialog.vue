<template>
  <el-dialog :model-value="visible" :title="isEdit ? '编辑菜品' : '新增菜品'" @close="handleClose" :width="isMobile ? '92vw' : '500px'">
    <el-form :model="form" label-width="80px">
      <el-form-item label="分类">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="菜品名">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="价格(元)">
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="form.stock" :min="0" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" />
      </el-form-item>
      <el-form-item label="图片">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          accept="image/*"
        >
          <img v-if="form.image" :src="form.image" alt="菜品图片" :style="isMobile ? 'width:60px;height:60px;object-fit:cover' : 'width:100px;height:100px;object-fit:cover'" />
          <el-button v-else size="small">上传图片</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { addDish, updateDish } from '../api/dish'
import { uploadImage } from '../api/upload'

const props = defineProps({
  visible: Boolean,
  dish: Object,
  categories: Array
})
const emit = defineEmits(['update:visible', 'saved'])

const loading = ref(false)
const isEdit = ref(false)
const form = ref({})
const isMobile = ref(window.innerWidth <= 768)

const handleResize = () => { isMobile.value = window.innerWidth <= 768 }
onMounted(() => { window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })

watch(() => props.dish, (val) => {
  if (val) {
    isEdit.value = true
    form.value = { ...val, price: val.price / 100 } // 分→元
  } else {
    isEdit.value = false
    form.value = { categoryId: null, name: '', price: 0, stock: 0, sortOrder: 0, image: '', description: '' }
  }
}, { immediate: true })

const beforeUpload = (file) => {
  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg、png、webp 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片不能超过 2MB')
    return false
  }
  return true
}

const handleUpload = async ({ file }) => {
  try {
    const res = await uploadImage(file)
    form.value.image = res.data
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

const handleSubmit = async () => {
  loading.value = true
  try {
    if (isEdit.value) {
      await updateDish({ ...form.value, price: Math.round(form.value.price * 100) })
    } else {
      await addDish({ ...form.value, price: Math.round(form.value.price * 100) })
    }
    ElMessage.success('保存成功')
    emit('saved')
    handleClose()
  } finally {
    loading.value = false
  }
}

const handleClose = () => emit('update:visible', false)
</script>
