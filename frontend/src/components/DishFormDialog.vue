<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑菜品' : '新增菜品'"
    :width="isMobile ? '92vw' : '520px'"
    @close="handleClose"
  >
    <el-form :model="form" label-width="96px">
      <el-form-item label="分类">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="菜品名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="整份价格(元)">
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width:100%" />
      </el-form-item>
      <el-form-item label="支持半份">
        <el-switch v-model="form.allowHalfPortion" />
      </el-form-item>
      <el-form-item v-if="form.allowHalfPortion" label="半份价格(元)">
        <el-input-number v-model="form.halfPrice" :min="0" :precision="2" :step="1" style="width:100%" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="form.stock" :min="0" style="width:100%" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
      </el-form-item>
      <el-form-item label="图片">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          accept="image/*"
        >
          <img
            v-if="form.image"
            :src="form.image"
            alt="菜品图片"
            :style="isMobile ? 'width:60px;height:60px;object-fit:cover;border-radius:12px' : 'width:100px;height:100px;object-fit:cover;border-radius:16px'"
          />
          <el-button v-else size="small">上传图片</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
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
  categories: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible', 'saved'])

const loading = ref(false)
const isEdit = ref(false)
const isMobile = ref(window.innerWidth <= 768)
const form = ref(createEmptyForm())

function createEmptyForm() {
  return {
    categoryId: null,
    name: '',
    price: 0,
    allowHalfPortion: false,
    halfPrice: 0,
    stock: 0,
    sortOrder: 0,
    image: '',
    description: ''
  }
}

function syncForm(dish) {
  if (!dish) {
    isEdit.value = false
    form.value = createEmptyForm()
    return
  }

  isEdit.value = true
  form.value = {
    ...createEmptyForm(),
    ...dish,
    price: Number(dish.price || 0) / 100,
    allowHalfPortion: Number(dish.allowHalfPortion || 0) === 1,
    halfPrice: Number(dish.halfPrice || 0) / 100
  }
}

watch(() => props.dish, syncForm, { immediate: true })

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

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
  } catch (error) {
    ElMessage.error('图片上传失败')
  }
}

const validateForm = () => {
  if (!form.value.categoryId) {
    ElMessage.error('请选择分类')
    return false
  }
  if (!String(form.value.name || '').trim()) {
    ElMessage.error('请输入菜品名称')
    return false
  }
  if (Number(form.value.price) <= 0) {
    ElMessage.error('整份价格必须大于 0')
    return false
  }
  if (form.value.allowHalfPortion) {
    if (Number(form.value.halfPrice) <= 0) {
      ElMessage.error('请输入有效的半份价格')
      return false
    }
    if (Number(form.value.halfPrice) > Number(form.value.price)) {
      ElMessage.error('半份价格不能高于整份价格')
      return false
    }
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  loading.value = true
  try {
    const payload = {
      ...form.value,
      name: String(form.value.name || '').trim(),
      allowHalfPortion: !!form.value.allowHalfPortion,
      halfPrice: form.value.allowHalfPortion ? form.value.halfPrice : null
    }

    if (isEdit.value) {
      await updateDish(payload)
    } else {
      await addDish(payload)
    }

    ElMessage.success('保存成功')
    emit('saved')
    handleClose()
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>
