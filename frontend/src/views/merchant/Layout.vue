<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #304156">
      <div style="color: #fff; text-align: center; padding: 20px 0; font-size: 16px">
        铜锅涮肉
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/m/dashboard">
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/m/dishes">
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/m/tables">
          <span>桌台管理</span>
        </el-menu-item>
        <el-menu-item index="/m/orders">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/m/sessions">
          <span>会话结账</span>
        </el-menu-item>
        <el-menu-item index="/m/manual-order">
          <span>手动下单</span>
        </el-menu-item>
        <el-menu-item index="/m/customers">
          <span>客户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex; justify-content:flex-end; align-items:center; gap:12px; background:#fff; border-bottom:1px solid #eee">
        <span>{{ user.username || user.name }}</span>
        <el-button @click="handleLogout" text>退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '../../api/auth'

const router = useRouter()
const user = computed(() => JSON.parse(sessionStorage.getItem('merchantUser') || '{}'))
const handleLogout = async () => {
  try {
    await logout()
  } finally {
    sessionStorage.removeItem('merchantUser')
    router.push('/m/login')
  }
}
</script>
