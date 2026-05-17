const { getTableDetail } = require('../../api/table')
const { ensureCustomerLogin, getCustomerProfile } = require('../../utils/auth')
const { extractTableId, extractTableIdFromScanResult } = require('../../utils/table-route')
const { maskPhone } = require('../../utils/format')

function showToast(title) {
  wx.showToast({
    title,
    icon: 'none',
    duration: 2200
  })
}

Page({
  data: {
    tableId: '',
    tableInfo: null,
    tableReady: false,
    loading: false,
    submitting: false,
    scanLoading: false,
    loginReady: false,
    loginError: '',
    customerLabel: '微信顾客'
  },

  onLoad(options) {
    const initialTableId = extractTableId(options) || Number(wx.getStorageSync('currentTableId') || 0)

    if (initialTableId) {
      this.loadTableInfo(initialTableId, { clearOnFail: true })
    }

    this.loginSilently()
  },

  async loadTableInfo(tableId, options) {
    const clearOnFail = options && options.clearOnFail

    this.setData({ loading: true })
    try {
      const tableInfo = await getTableDetail(tableId)
      this.setData({
        tableId: String(tableId),
        tableInfo,
        tableReady: true
      })
      wx.setStorageSync('currentTableId', tableId)
      return true
    } catch (error) {
      if (clearOnFail) {
        this.setData({
          tableId: '',
          tableInfo: null,
          tableReady: false
        })
        wx.removeStorageSync('currentTableId')
      }
      return false
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
      const customerLabel = customer.phoneBound && customer.phone
        ? maskPhone(customer.phone)
        : (customer.name || '微信顾客')

      this.setData({
        loginReady: true,
        customerLabel
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

  async applyTableId(tableId) {
    const normalizedTableId = Number(tableId)

    if (!normalizedTableId) {
      showToast('没有识别到有效桌号')
      return false
    }

    const loaded = await this.loadTableInfo(normalizedTableId, { clearOnFail: false })
    if (!loaded) {
      showToast('未找到对应桌台，请核对桌码')
      return false
    }

    return true
  },

  async handleScanTable() {
    if (this.data.scanLoading) {
      return
    }

    this.setData({ scanLoading: true })

    try {
      const scanResult = await new Promise((resolve, reject) => {
        wx.scanCode({
          onlyFromCamera: true,
          scanType: ['qrCode'],
          success: resolve,
          fail: reject
        })
      })

      const tableId = extractTableIdFromScanResult(scanResult)
      if (!tableId) {
        showToast('二维码里没有可识别的桌号')
        return
      }

      const success = await this.applyTableId(tableId)
      if (success) {
        wx.showToast({
          title: '已识别桌台',
          icon: 'success'
        })
      }
    } catch (error) {
      if (error && error.errMsg && error.errMsg.includes('cancel')) {
        return
      }
      showToast('扫码失败，请检查相机权限')
    } finally {
      this.setData({ scanLoading: false })
    }
  },

  async handleSubmit() {
    const tableId = Number(this.data.tableId)

    if (!tableId || !this.data.tableReady) {
      showToast('请先扫码选择桌台')
      return
    }

    if (!this.data.loginReady) {
      await this.loginSilently()
      if (!this.data.loginReady) {
        return
      }
    }

    wx.redirectTo({
      url: `/pages/menu/index?tableId=${tableId}`
    })
  },

  goMenu() {
    this.handleSubmit()
  },

  goStatus() {
    const tableId = Number(this.data.tableId || 0)
    if (!tableId || !this.data.tableReady) {
      showToast('请先扫码选择桌台')
      return
    }

    wx.redirectTo({
      url: `/pages/status/index?tableId=${tableId}`
    })
  },

  goMine() {
    const tableId = Number(this.data.tableId || wx.getStorageSync('currentTableId') || 0)
    const url = tableId ? `/pages/mine/index?tableId=${tableId}` : '/pages/mine/index'
    wx.redirectTo({ url })
  },

  retryLogin() {
    this.loginSilently()
  }
})
