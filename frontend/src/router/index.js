import { createRouter, createWebHistory } from 'vue-router'

const MerchantLogin = () => import('../views/merchant/Login.vue')
const MerchantLayout = () => import('../views/merchant/Layout.vue')
const Dashboard = () => import('../views/merchant/Dashboard.vue')
const Dishes = () => import('../views/merchant/Dishes.vue')
const Tables = () => import('../views/merchant/Tables.vue')
const Orders = () => import('../views/merchant/Orders.vue')
const Customers = () => import('../views/merchant/Customers.vue')
const ManualOrder = () => import('../views/merchant/ManualOrder.vue')
const History = () => import('../views/merchant/History.vue')
const TablesView = () => import('../views/merchant/TablesView.vue')
const Statistics = () => import('../views/merchant/Statistics.vue')

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
      { path: 'sessions', redirect: (to) => ({ path: '/m/tables-view', query: to.query }) },
      { path: 'customers', name: 'MerchantCustomers', component: Customers },
      { path: 'manual-order', name: 'MerchantManualOrder', component: ManualOrder },
      { path: 'history', name: 'MerchantHistory', component: History },
      { path: 'tables-view', name: 'MerchantTablesView', component: TablesView },
      { path: 'statistics', name: 'MerchantStatistics', component: Statistics },
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
