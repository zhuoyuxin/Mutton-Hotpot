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
  { path: '/m/login', name: 'MerchantLogin', component: MerchantLogin },
  {
    path: '/m',
    name: 'MerchantLayout',
    component: MerchantLayout,
    meta: { requiresMerchantAuth: true },
    children: [
      { path: '', redirect: '/m/dashboard' },
      { path: 'dashboard', name: 'MerchantDashboard', component: Dashboard },
      { path: 'dishes', name: 'MerchantDishes', component: Dishes },
      { path: 'tables', name: 'MerchantTables', component: Tables },
      { path: 'orders', name: 'MerchantOrders', component: Orders },
      { path: 'sessions', name: 'MerchantSessions', component: Sessions },
      { path: 'customers', name: 'MerchantCustomers', component: Customers },
      { path: 'manual-order', name: 'MerchantManualOrder', component: ManualOrder },
    ]
  },
  { path: '/c/login/:tableId', name: 'CustomerLogin', component: CustomerLogin },
  { path: '/c/order/:tableId', name: 'CustomerOrder', component: CustomerOrder },
  { path: '/c/status/:tableId', name: 'CustomerStatus', component: CustomerStatus },
  { path: '/c/mine', name: 'CustomerMine', component: CustomerMine },
  { path: '/:pathMatch(.*)*', name: 'NotFound', redirect: '/m/login' },
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
