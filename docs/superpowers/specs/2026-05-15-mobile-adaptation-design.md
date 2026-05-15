# 移动端适配设计文档

## 目标

商户端全面移动端适配（手机+平板），客户端补全安全区域等小问题。

## 方案

保持 Element Plus 现有组件风格，通过响应式断点 + CSS media queries 做布局调整。

## 设计细节

### 1. 全局基础层

新增 `src/styles/mobile.css`，在 `main.js` 中引入：

- `box-sizing: border-box` 全局重置
- `-webkit-tap-highlight-color: transparent` 触摸优化
- CSS 变量 `--safe-top` / `--safe-bottom` 安全区域

### 2. 商户端 Layout.vue

- `isMobile = ref(window.innerWidth <= 768)` + resize 监听
- 桌面：保持现有左侧固定侧边栏
- 移动端：隐藏侧边栏，header 显示汉堡按钮，点击弹出 el-drawer 菜单，选中后自动关闭

### 3. el-col 响应式断点

| 页面 | 当前 | 改为 |
|------|------|------|
| Dashboard | :span="6" | :xs="12" :sm="12" :md="6" |
| Sessions | :span="6" | :xs="12" :sm="8" :md="6" |
| ManualOrder | :span="6" | :xs="12" :sm="8" :md="6" |
| Dishes 分类 | :span="12" | :xs="24" :md="12" |

### 4. el-dialog 响应式宽度

所有固定宽度 dialog 改为 `isMobile ? '92vw' : 原宽度`：
- DishFormDialog 500px
- Dishes 分类弹窗 300px
- Tables 弹窗 400px/350px
- Customers 弹窗 500px/400px
- Sessions 结账弹窗 500px

### 5. el-table 横向滚动

每个 el-table 父容器加 `.table-wrapper { overflow-x: auto; -webkit-overflow-scrolling: touch; }`

### 6. 客户端修复

- Login.vue：顶部安全区域内边距
- Status.vue：底部安全区域 + 标题 sticky
- Mine.vue：顶部安全区域
- 全局：box-sizing、文本溢出处理

## 影响文件

- `src/styles/mobile.css`（新增）
- `src/main.js`
- `src/views/merchant/Layout.vue`
- `src/views/merchant/Login.vue`
- `src/views/merchant/Dashboard.vue`
- `src/views/merchant/Dishes.vue`
- `src/views/merchant/Tables.vue`
- `src/views/merchant/Orders.vue`
- `src/views/merchant/Sessions.vue`
- `src/views/merchant/Customers.vue`
- `src/views/merchant/ManualOrder.vue`
- `src/components/DishFormDialog.vue`
- `src/views/customer/Login.vue`
- `src/views/customer/Status.vue`
- `src/views/customer/Mine.vue`
