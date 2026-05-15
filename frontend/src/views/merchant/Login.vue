<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 style="text-align:center; margin-bottom:20px">铜锅涮肉管理系统</h2>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" native-type="submit" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 强制改密弹窗 -->
      <el-dialog v-model="showChangePwd" title="首次登录，请修改密码" :close-on-click-modal="false" :show-close="false">
        <el-form :model="pwdForm">
          <el-form-item label="旧密码">
            <el-input v-model="pwdForm.oldPassword" type="password" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="confirmPwd" type="password" placeholder="请再次输入新密码" autocomplete="new-password" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" @click="handleChangePwd" :loading="loading">确认修改</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, changePassword } from '../../api/auth'

const router = useRouter()
const loading = ref(false)
const showChangePwd = ref(false)
const form = ref({ username: '', password: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '' })
const confirmPwd = ref('')

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await login(form.value)
    sessionStorage.setItem('merchantUser', JSON.stringify(res.data))
    if (res.data.mustChangePassword === 1) {
      pwdForm.value.oldPassword = form.value.password
      showChangePwd.value = true
    } else {
      router.push('/m/dashboard')
    }
  } catch {
    form.value.password = ''
  } finally {
    loading.value = false
  }
}

const handleChangePwd = async () => {
  if (pwdForm.value.newPassword !== confirmPwd.value) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await changePassword(pwdForm.value)
    ElMessage.success('密码修改成功')
    confirmPwd.value = ''
    showChangePwd.value = false
    router.push('/m/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f5f5;
}
.login-card {
  width: 90%;
  max-width: 400px;
}
</style>
