<template>
  <el-container style="height: 100vh">
    <el-aside v-if="!isMobile" width="200px" style="background: #304156">
      <div class="sidebar-title">铜锅涮肉</div>
      <el-menu
        :default-active="activeMenu"
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
          <span>桌台配置</span>
        </el-menu-item>
        <el-menu-item index="/m/tables-view">
          <span>桌台运营</span>
        </el-menu-item>
        <el-menu-item index="/m/orders">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/m/manual-order">
          <span>手动下单</span>
        </el-menu-item>
        <el-menu-item index="/m/customers">
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/m/history">
          <span>结账历史</span>
        </el-menu-item>
        <el-menu-item index="/m/statistics">
          <span>营业统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      :show-close="false"
      :with-header="false"
      size="200px"
      class="mobile-drawer"
    >
      <div class="sidebar-title">铜锅涮肉</div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="onMenuSelect"
      >
        <el-menu-item index="/m/dashboard">
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/m/dishes">
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/m/tables">
          <span>桌台配置</span>
        </el-menu-item>
        <el-menu-item index="/m/tables-view">
          <span>桌台运营</span>
        </el-menu-item>
        <el-menu-item index="/m/orders">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/m/manual-order">
          <span>手动下单</span>
        </el-menu-item>
        <el-menu-item index="/m/customers">
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/m/history">
          <span>结账历史</span>
        </el-menu-item>
        <el-menu-item index="/m/statistics">
          <span>营业统计</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-container>
      <el-header class="app-header">
        <el-button
          v-if="isMobile"
          class="hamburger-btn"
          :icon="Menu"
          text
          @click="drawerVisible = true"
        />
        <span v-if="isMobile" class="mobile-title">铜锅涮肉</span>
        <span class="spacer"></span>
        <span class="user-name">{{ user.username || user.name }}</span>
        <el-button @click="handleLogout" text>退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Menu } from '@element-plus/icons-vue'
import { logout } from '../../api/auth'

const router = useRouter()
const route = useRoute()
const user = computed(() => JSON.parse(sessionStorage.getItem('merchantUser') || '{}'))

const isMobile = ref(window.innerWidth <= 768)
const drawerVisible = ref(false)

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    drawerVisible.value = false
  }
}

onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => window.removeEventListener('resize', handleResize))

const activeMenu = computed(() => route.path)

const onMenuSelect = (index) => {
  if (isMobile.value) {
    drawerVisible.value = false
  }
  router.push(index)
}

const handleLogout = async () => {
  try {
    await logout()
  } finally {
    sessionStorage.removeItem('merchantUser')
    router.push('/m/login')
  }
}
</script>

<style scoped>
.sidebar-title {
  color: #fff;
  text-align: center;
  padding: 20px 0;
  font-size: 16px;
}

.app-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.spacer {
  flex: 1;
}

.hamburger-btn {
  font-size: 20px;
  padding: 4px 8px;
}

.mobile-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.user-name {
  white-space: nowrap;
}

.mobile-drawer :deep(.el-drawer__body) {
  background-color: #304156;
  padding: 0;
}

@media (max-width: 768px) {
  .el-main {
    padding: 10px;
  }
}
</style>
