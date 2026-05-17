const { customerInfo, customerOrders, customerPoints, bindCustomerPhone } = require('../../api/customer-api')
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

function isValidMobilePhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

Page({
  data: {
    tableId: '',
    loading: false,
    bindLoading: false,
    isLoggedIn: false,
    info: null,
    maskedPhone: '',
    totalSpentText: '0.00',
    orders: [],
    pointsRecords: [],
    activeTab: 'orders',
    bindingPhone: ''
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

  cacheCustomerProfile(info) {
    const cachedProfile = getCustomerProfile() || {}
    const nextProfile = {
      ...cachedProfile,
      ...(info || {})
    }
    wx.setStorageSync('customerProfile', nextProfile)
    return nextProfile
  },

  async loadData() {
    this.setData({ loading: true })
    try {
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
          pointsRecords: [],
          bindingPhone: ''
        })
        return
      }

      const cachedProfile = this.cacheCustomerProfile(info)
      this.setData({
        isLoggedIn: true,
        info,
        maskedPhone: info.phoneBound && info.phone ? maskPhone(info.phone) : (cachedProfile.name || info.name || '微信顾客'),
        totalSpentText: formatPrice(info.totalSpent),
        bindingPhone: info.phone || '',
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
        pointsRecords: [],
        bindingPhone: ''
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  handlePhoneInput(event) {
    const value = String(event.detail.value || '').replace(/\D/g, '').slice(0, 11)
    this.setData({ bindingPhone: value })
  },

  async submitPhoneBinding() {
    if (!this.data.isLoggedIn || this.data.bindLoading) {
      return
    }

    const phone = String(this.data.bindingPhone || '').trim()
    if (!isValidMobilePhone(phone)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }

    this.setData({ bindLoading: true })
    try {
      const info = await bindCustomerPhone({ phone })
      this.cacheCustomerProfile(info)
      wx.showToast({
        title: '绑定成功',
        icon: 'success'
      })
      await this.loadData()
    } finally {
      this.setData({ bindLoading: false })
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
