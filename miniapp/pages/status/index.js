const { getTableOrders } = require('../../api/order')
const { getTableDetail } = require('../../api/table')
const { extractTableId } = require('../../utils/navigation')
const { ORDER_STATUS_TEXT, ITEM_STATUS_TEXT } = require('../../utils/order')
const { formatDateTime } = require('../../utils/format')

function orderBadgeClass(status) {
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

function itemBadgeClass(status) {
  if (status === 2) {
    return 'success'
  }
  if (status === 3 || status === 4) {
    return 'danger'
  }
  return 'processing'
}

Page({
  pollingTimer: null,

  data: {
    tableId: '',
    tableInfo: null,
    loading: false,
    orders: [],
    totalOrders: 0,
    pendingItems: 0,
    servedItems: 0,
    lastUpdatedAt: ''
  },

  onLoad(options) {
    const tableId = extractTableId(options) || Number(wx.getStorageSync('currentTableId') || 0)
    if (!tableId) {
      wx.redirectTo({ url: '/pages/entry/index' })
      return
    }

    wx.setStorageSync('currentTableId', tableId)
    this.setData({ tableId: String(tableId) })
  },

  onShow() {
    this.loadData()
    this.startPolling()
  },

  onHide() {
    this.stopPolling()
  },

  onUnload() {
    this.stopPolling()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  async loadData() {
    const tableId = Number(this.data.tableId || wx.getStorageSync('currentTableId') || 0)
    if (!tableId) {
      return
    }

    this.setData({ loading: true })
    try {
      const [tableInfo, orderList] = await Promise.all([
        getTableDetail(tableId).catch(() => null),
        getTableOrders(tableId)
      ])

      const orders = (orderList || []).map((order) => ({
        ...order,
        statusText: ORDER_STATUS_TEXT[order.status] || '未知状态',
        statusClass: orderBadgeClass(order.status),
        createTimeText: formatDateTime(order.createTime),
        items: (order.items || []).map((item) => ({
          ...item,
          statusText: ITEM_STATUS_TEXT[item.status] || '处理中',
          statusClass: itemBadgeClass(item.status)
        }))
      }))

      const allItems = orders.flatMap((order) => order.items || [])
      const pendingItems = allItems.filter((item) => item.status === 0 || item.status === 1).length
      const servedItems = allItems.filter((item) => item.status === 2).length

      this.setData({
        tableInfo,
        orders,
        totalOrders: orders.length,
        pendingItems,
        servedItems,
        lastUpdatedAt: formatDateTime(new Date())
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  startPolling() {
    this.stopPolling()
    this.pollingTimer = setInterval(() => {
      this.loadData()
    }, 10000)
  },

  stopPolling() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer)
      this.pollingTimer = null
    }
  },

  goMenu() {
    wx.redirectTo({
      url: `/pages/menu/index?tableId=${this.data.tableId}`
    })
  },

  goMine() {
    wx.redirectTo({
      url: `/pages/mine/index?tableId=${this.data.tableId}`
    })
  }
})
