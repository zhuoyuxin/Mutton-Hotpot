<template>
  <div class="customer-page">
    <div class="login-box">
      <h2 style="text-align:center; margin-bottom:20px; color:#333">铜锅涮肉</h2>
      <p style="text-align:center; color:#999; margin-bottom:30px">扫码点餐</p>

      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="phone" placeholder="输入手机号（可选）" size="large" maxlength="11" clearable />
        </el-form-item>
        <el-button type="danger" size="large" style="width:100%; margin-top:10px" native-type="submit">
          进入点餐
        </el-button>
        <p style="text-align:center; color:#999; margin-top:15px; font-size:12px">
          不填手机号可直接点餐，但无法查看积分和消费记录
        </p>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { customerLogin } from '../../api/customer'

const route = useRoute()
const router = useRouter()
const phone = ref('')

const handleLogin = async () => {
  if (phone.value && !/^1[3-9]\d{9}$/.test(phone.value)) {
    ElMessage.error('请输入正确的手机号')
    return
  }
  try {
    if (phone.value) {
      await customerLogin({ phone: phone.value })
      localStorage.setItem('customerPhone', phone.value)
    }
    localStorage.setItem('currentTableId', route.params.tableId)
    router.push('/c/order/' + route.params.tableId)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '登录失败，请重试')
  }
}
</script>

<style scoped>
.customer-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: calc(60px + var(--safe-top, 0px)) 20px 20px;
}
.login-box {
  max-width: 400px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 30px 20px;
}
</style>
