import { createRouter, createWebHistory } from 'vue-router'

const MerchantLogin = () => import('../views/merchant/Login.vue')
const MerchantLayout = () => import('../views/merchant/Layout.vue')
const Dashboard = () => import('../views/merchant/Dashboard.vue')
const Dishes = () => import('../views/merchant/Dishes.vue')
const Tables = () => import('../views/merchant/Tables.vue')
const Orders = () => import('../views/merchant/Orders.vue')
const Sessions = () => import('../views/merchant/Sessions.vue')
const Customers = () => import('../views/merchant/Customers.vue')
const ManualOrder = () => import('../views/merchant/ManualOrder.vue')

const CustomerLogin = () => import('../views/customer/Login.vue')
const CustomerOrder = () => import('../views/customer/Order.vue')
const CustomerStatus = () => import('../views/customer/Status.vue')
const CustomerMine = () => import('../views/customer/Mine.vue')

const routes = [
  { path: '/m/login', component: MerchantLogin },
  {
    path: '/m',
    component: MerchantLayout,
    meta: { requiresMerchantAuth: true },
    children: [
      { path: '', redirect: '/m/dashboard' },
      { path: 'dashboard', component: Dashboard },
      { path: 'dishes', component: Dishes },
      { path: 'tables', component: Tables },
      { path: 'orders', component: Orders },
      { path: 'sessions', component: Sessions },
      { path: 'customers', component: Customers },
      { path: 'manual-order', component: ManualOrder },
    ]
  },
  { path: '/c/login/:tableId', component: CustomerLogin },
  { path: '/c/order/:tableId', component: CustomerOrder },
  { path: '/c/status/:tableId', component: CustomerStatus },
  { path: '/c/mine', component: CustomerMine },
  { path: '/', redirect: '/m/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.matched.some(r => r.meta.requiresMerchantAuth)) {
    const merchant = sessionStorage.getItem('merchantUser')
    if (!merchant) {
      next('/m/login')
      return
    }
  }
  next()
})

export default router
