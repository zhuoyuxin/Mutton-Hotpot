const { getTableDetail } = require('../../api/table')
const { ensureCustomerLogin, getCustomerProfile } = require('../../utils/auth')
const { extractTableId } = require('../../utils/navigation')

Page({
  data: {
    tableId: '',
    tableInfo: null,
    loading: false,
    submitting: false,
    loginReady: false,
    loginError: '',
    customerLabel: '微信顾客'
  },

  onLoad(options) {
    const initialTableId = extractTableId(options) || Number(wx.getStorageSync('currentTableId') || 0)

    this.setData({
      tableId: initialTableId ? String(initialTableId) : ''
    })

    if (initialTableId) {
      wx.setStorageSync('currentTableId', initialTableId)
      this.loadTableInfo(initialTableId)
    }

    this.loginSilently()
  },

  handleTableIdInput(event) {
    const value = String(event.detail.value || '').replace(/[^\d]/g, '')
    this.setData({ tableId: value })
  },

  async handleTableBlur() {
    const tableId = Number(this.data.tableId)
    if (!tableId) {
      this.setData({ tableInfo: null })
      return
    }
    wx.setStorageSync('currentTableId', tableId)
    await this.loadTableInfo(tableId)
  },

  async loadTableInfo(tableId) {
    this.setData({ loading: true })
    try {
      const tableInfo = await getTableDetail(tableId)
      this.setData({ tableInfo })
    } catch (error) {
      this.setData({ tableInfo: null })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loginSilently() {
    this.setData({
      submitting: true,
      loginError: ''
    })
    try {
      const authData = await ensureCustomerLogin()
      const customer = authData.customer || getCustomerProfile() || {}
      this.setData({
        loginReady: true,
        customerLabel: customer.name || '微信顾客'
      })
    } catch (error) {
      this.setData({
        loginReady: false,
        loginError: error && error.message ? error.message : '微信登录失败，请稍后重试'
      })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async handleSubmit() {
    const tableId = Number(this.data.tableId)

    if (!tableId) {
      wx.showToast({
        title: '请先输入桌号',
        icon: 'none'
      })
      return
    }

    if (!this.data.loginReady) {
      await this.loginSilently()
      if (!this.data.loginReady) {
        return
      }
    }

    wx.setStorageSync('currentTableId', tableId)
    wx.redirectTo({
      url: `/pages/menu/index?tableId=${tableId}`
    })
  },

  retryLogin() {
    this.loginSilently()
  }
})
