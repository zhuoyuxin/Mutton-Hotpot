const { customerLogin } = require('../../api/customer')
const { getTableDetail } = require('../../api/table')
const { extractTableId } = require('../../utils/navigation')

Page({
  data: {
    tableId: '',
    phone: '',
    tableInfo: null,
    loading: false,
    submitting: false
  },

  onLoad(options) {
    const initialTableId = extractTableId(options) || Number(wx.getStorageSync('currentTableId') || 0)
    const savedPhone = wx.getStorageSync('customerPhone') || ''

    this.setData({
      tableId: initialTableId ? String(initialTableId) : '',
      phone: savedPhone
    })

    if (initialTableId) {
      wx.setStorageSync('currentTableId', initialTableId)
      this.loadTableInfo(initialTableId)
    }
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

  handlePhoneInput(event) {
    this.setData({
      phone: String(event.detail.value || '').replace(/\s/g, '')
    })
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

  async handleSubmit() {
    const tableId = Number(this.data.tableId)
    const phone = String(this.data.phone || '').trim()

    if (!tableId) {
      wx.showToast({
        title: '请先输入桌号',
        icon: 'none'
      })
      return
    }

    if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      })
      return
    }

    this.setData({ submitting: true })
    try {
      if (phone) {
        await customerLogin({ phone })
        wx.setStorageSync('customerPhone', phone)
      } else {
        wx.removeStorageSync('customerPhone')
      }

      wx.setStorageSync('currentTableId', tableId)
      wx.redirectTo({
        url: `/pages/menu/index?tableId=${tableId}`
      })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
