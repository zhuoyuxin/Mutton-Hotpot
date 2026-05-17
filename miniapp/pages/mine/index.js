const { customerInfo, customerOrders, customerPoints } = require('../../api/customer')
const { ensureCustomerLogin, getCustomerProfile } = require('../../utils/auth')
const { extractTableId } = require('../../utils/table-route')
const { formatPrice, maskPhone, formatDateTime } = require('../../utils/format')
const { ORDER_STATUS_TEXT } = require('../../utils/order')

function recordStatusClass(status) {
  if (status === 3 || status === 4) {
    return 'success'
  }
  if (status === 5) {
    return 'muted'
  }
  if (status === 2) {
    return 'warning'
  }
  return 'processing'
}

Page({
  data: {
    tableId: '',
    loading: false,
    isLoggedIn: false,
    info: null,
    maskedPhone: '',
    totalSpentText: '0.00',
    orders: [],
    pointsRecords: [],
    activeTab: 'orders'
  },

  onLoad(options) {
    const tableId = extractTableId(options) || Number(wx.getStorageSync('currentTableId') || 0)
    if (tableId) {
      wx.setStorageSync('currentTableId', tableId)
      this.setData({ tableId: String(tableId) })
    }
  },

  onShow() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const cachedProfile = getCustomerProfile() || {}
      if (!wx.getStorageSync('customerToken')) {
        await ensureCustomerLogin()
      }

      const [infoResult, ordersResult, pointsResult] = await Promise.allSettled([
        customerInfo(),
        customerOrders(),
        customerPoints()
      ])

      const info = infoResult.status === 'fulfilled' ? infoResult.value : null
      const orderList = ordersResult.status === 'fulfilled' ? ordersResult.value : []
      const pointList = pointsResult.status === 'fulfilled' ? pointsResult.value : []

      if (!info) {
        this.setData({
          isLoggedIn: false,
          info: null,
          maskedPhone: '',
          totalSpentText: '0.00',
          orders: [],
          pointsRecords: []
        })
        return
      }

      this.setData({
        isLoggedIn: true,
        info,
        maskedPhone: info.phoneBound && info.phone ? maskPhone(info.phone) : (cachedProfile.name || info.name || '微信顾客'),
        totalSpentText: formatPrice(info.totalSpent),
        orders: orderList.map((order) => ({
          ...order,
          statusText: ORDER_STATUS_TEXT[order.status] || '未知状态',
          statusClass: recordStatusClass(order.status),
          totalAmountText: formatPrice(order.totalAmount),
          createTimeText: formatDateTime(order.createTime)
        })),
        pointsRecords: pointList.map((record) => ({
          ...record,
          pointsText: `${record.points > 0 ? '+' : ''}${record.points}`,
          createTimeText: formatDateTime(record.createTime)
        }))
      })
    } catch (error) {
      this.setData({
        isLoggedIn: false,
        info: null,
        maskedPhone: '',
        totalSpentText: '0.00',
        orders: [],
        pointsRecords: []
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  switchTab(event) {
    const activeTab = event.currentTarget.dataset.tab
    this.setData({ activeTab })
  },

  goEntry() {
    const tableId = this.data.tableId || wx.getStorageSync('currentTableId') || ''
    const url = tableId ? `/pages/entry/index?tableId=${tableId}` : '/pages/entry/index'
    wx.redirectTo({ url })
  },

  goMenu() {
    const tableId = this.data.tableId || wx.getStorageSync('currentTableId') || ''
    if (!tableId) {
      wx.redirectTo({ url: '/pages/entry/index' })
      return
    }
    wx.redirectTo({ url: `/pages/menu/index?tableId=${tableId}` })
  },

  goStatus() {
    const tableId = this.data.tableId || wx.getStorageSync('currentTableId') || ''
    if (!tableId) {
      wx.redirectTo({ url: '/pages/entry/index' })
      return
    }
    wx.redirectTo({ url: `/pages/status/index?tableId=${tableId}` })
  }
})
