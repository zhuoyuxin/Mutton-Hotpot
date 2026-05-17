const { getCustomerDishes } = require('../../api/dish')
const { createCustomerOrder } = require('../../api/order')
const { getTableDetail } = require('../../api/table')
const { ensureCustomerLogin, getCustomerProfile } = require('../../utils/auth')
const { extractTableId } = require('../../utils/table-route')
const { formatPrice, maskPhone } = require('../../utils/format')
const {
  PORTION_FULL,
  PORTION_HALF,
  buildDishDisplayName,
  createCartKey,
  getPortionLabel,
  getPortionPrice,
  normalizePortionType,
  parseCartKey,
  supportsHalfPortion
} = require('../../utils/portion')

function showToast(title) {
  wx.showToast({
    title,
    icon: 'none',
    duration: 2200
  })
}

function getDishSelectedQty(cart, dishId) {
  return Object.keys(cart).reduce((sum, cartKey) => {
    const parsed = parseCartKey(cartKey)
    if (parsed.dishId !== Number(dishId)) {
      return sum
    }
    return sum + Number(cart[cartKey] || 0)
  }, 0)
}

function getCartQty(cart, dishId, portionType) {
  return Number(cart[createCartKey(dishId, portionType)] || 0)
}

function normalizeSavedCart(savedCart) {
  const nextCart = {}
  Object.keys(savedCart || {}).forEach((cartKey) => {
    const parsed = parseCartKey(cartKey)
    const quantity = Number(savedCart[cartKey] || 0)
    if (parsed.dishId > 0 && quantity > 0) {
      nextCart[createCartKey(parsed.dishId, parsed.portionType)] = quantity
    }
  })
  return nextCart
}

function buildPortionOptions(dish, cart) {
  const totalSelectedQty = getDishSelectedQty(cart, dish.id)
  const options = [
    {
      portionType: PORTION_FULL,
      label: '整份',
      price: Number(dish.price || 0),
      priceText: formatPrice(dish.price),
      cartQty: getCartQty(cart, dish.id, PORTION_FULL)
    }
  ]

  if (supportsHalfPortion(dish)) {
    options.push({
      portionType: PORTION_HALF,
      label: '半份',
      price: Number(dish.halfPrice || 0),
      priceText: formatPrice(dish.halfPrice),
      cartQty: getCartQty(cart, dish.id, PORTION_HALF)
    })
  }

  return options.map((option) => ({
    ...option,
    addDisabled: Number(dish.stock || 0) <= totalSelectedQty
  }))
}

Page({
  data: {
    tableId: '',
    tableInfo: null,
    loading: false,
    submitting: false,
    keyword: '',
    categories: [{ id: 0, name: '全部菜品' }],
    allDishes: [],
    dishes: [],
    activeCategoryId: 0,
    cart: {},
    cartItems: [],
    totalCount: 0,
    totalPrice: '0.00',
    showCart: false,
    remark: '',
    customerLabel: '微信顾客'
  },

  onLoad(options) {
    const tableId = extractTableId(options) || Number(wx.getStorageSync('currentTableId') || 0)
    if (!tableId) {
      wx.redirectTo({ url: '/pages/entry/index' })
      return
    }

    wx.setStorageSync('currentTableId', tableId)
    this.setData({ tableId: String(tableId) })
    this.loadCart(tableId)
  },

  onShow() {
    const tableId = Number(this.data.tableId || wx.getStorageSync('currentTableId') || 0)
    if (!tableId) {
      return
    }

    this.refreshCustomerProfile()
    ensureCustomerLogin()
      .then(() => {
        this.refreshCustomerProfile()
        return this.loadData(tableId)
      })
      .catch(() => {
        this.loadData(tableId)
      })
  },

  onPullDownRefresh() {
    const tableId = Number(this.data.tableId || 0)
    this.loadData(tableId).finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  refreshCustomerProfile() {
    const profile = getCustomerProfile() || {}
    const label = profile.phoneBound && profile.phone
      ? maskPhone(profile.phone)
      : (profile.name || '微信顾客')

    this.setData({
      customerLabel: label
    })
  },

  async loadData(tableId) {
    if (!tableId) {
      return
    }

    this.setData({ loading: true })
    try {
      const [tableInfo, dishData] = await Promise.all([
        getTableDetail(tableId).catch(() => null),
        getCustomerDishes()
      ])

      const categories = [{ id: 0, name: '全部菜品' }].concat(dishData.categories || [])
      const allDishes = (dishData.dishes || dishData || [])
        .filter((dish) => dish.status === undefined || dish.status === 1)
        .map((dish) => ({ ...dish }))

      this.setData({
        tableInfo,
        categories,
        allDishes
      })
      this.buildCartSummary()
      this.applyFilters()
    } finally {
      this.setData({ loading: false })
    }
  },

  handleKeywordInput(event) {
    this.setData({
      keyword: String(event.detail.value || '')
    })
    this.applyFilters()
  },

  selectCategory(event) {
    const activeCategoryId = Number(event.currentTarget.dataset.id || 0)
    this.setData({ activeCategoryId })
    this.applyFilters()
  },

  applyFilters() {
    const { allDishes, activeCategoryId, keyword, cart } = this.data
    const normalizedKeyword = String(keyword || '').trim().toLowerCase()

    const dishes = allDishes
      .filter((dish) => {
        const matchCategory = activeCategoryId === 0 || Number(dish.categoryId) === activeCategoryId
        const matchKeyword = !normalizedKeyword || String(dish.name || '').toLowerCase().includes(normalizedKeyword)
        return matchCategory && matchKeyword
      })
      .map((dish) => ({
        ...dish,
        supportsHalf: supportsHalfPortion(dish),
        totalCartQty: getDishSelectedQty(cart, dish.id),
        portionOptions: buildPortionOptions(dish, cart)
      }))

    this.setData({ dishes })
  },

  loadCart(tableId) {
    const savedCart = wx.getStorageSync(`cart_${tableId}`) || {}
    const cart = normalizeSavedCart(savedCart)
    this.setData({ cart }, () => {
      this.buildCartSummary()
      this.applyFilters()
    })
  },

  persistCart() {
    wx.setStorageSync(`cart_${this.data.tableId}`, this.data.cart)
  },

  changeQty(event) {
    const dishId = Number(event.currentTarget.dataset.id)
    const delta = Number(event.currentTarget.dataset.delta)
    const portionType = normalizePortionType(event.currentTarget.dataset.portion)
    const dish = this.data.allDishes.find((item) => item.id === dishId)

    if (!dish) {
      return
    }

    const cartKey = createCartKey(dishId, portionType)
    const currentQty = Number(this.data.cart[cartKey] || 0)
    const nextQty = currentQty + delta

    if (nextQty < 0) {
      return
    }

    if (delta > 0 && getDishSelectedQty(this.data.cart, dishId) >= Number(dish.stock || 0)) {
      showToast('已达到当前库存上限')
      return
    }

    const cart = { ...this.data.cart }
    if (nextQty === 0) {
      delete cart[cartKey]
    } else {
      cart[cartKey] = nextQty
    }

    this.setData({ cart }, () => {
      this.persistCart()
      this.buildCartSummary()
      this.applyFilters()
    })
  },

  buildCartSummary() {
    const { cart, allDishes } = this.data
    const cartItems = Object.keys(cart)
      .map((cartKey) => {
        const parsed = parseCartKey(cartKey)
        const dish = allDishes.find((item) => item.id === parsed.dishId)
        const qty = Number(cart[cartKey] || 0)
        if (!dish || qty <= 0) {
          return null
        }

        const price = getPortionPrice(dish, parsed.portionType)
        return {
          cartKey,
          id: dish.id,
          dishId: dish.id,
          name: dish.name,
          displayName: buildDishDisplayName(dish.name, parsed.portionType),
          portionType: parsed.portionType,
          portionLabel: getPortionLabel(parsed.portionType),
          price,
          priceText: formatPrice(price),
          qty,
          subtotalText: formatPrice(price * qty)
        }
      })
      .filter(Boolean)

    const totalCount = cartItems.reduce((sum, item) => sum + item.qty, 0)
    const totalPrice = cartItems.reduce((sum, item) => sum + Number(item.price || 0) * item.qty, 0)

    this.setData({
      cartItems,
      totalCount,
      totalPrice: formatPrice(totalPrice)
    })
  },

  toggleCart() {
    if (!this.data.totalCount) {
      return
    }
    this.setData({ showCart: !this.data.showCart })
  },

  closeCart() {
    this.setData({ showCart: false })
  },

  handleRemarkInput(event) {
    this.setData({
      remark: String(event.detail.value || '')
    })
  },

  async submitOrder() {
    const items = this.data.cartItems.map((item) => ({
      dishId: item.dishId,
      quantity: item.qty,
      portionType: item.portionType
    }))

    if (!items.length) {
      showToast('请先选择菜品')
      return
    }

    this.setData({ submitting: true })
    try {
      await createCustomerOrder({
        tableId: Number(this.data.tableId),
        items,
        remark: this.data.remark || ''
      })

      wx.showToast({
        title: '下单成功',
        icon: 'success'
      })

      wx.removeStorageSync(`cart_${this.data.tableId}`)
      this.setData(
        {
          cart: {},
          cartItems: [],
          totalCount: 0,
          totalPrice: '0.00',
          remark: '',
          showCart: false
        },
        () => {
          this.applyFilters()
        }
      )

      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/status/index?tableId=${this.data.tableId}`
        })
      }, 280)
    } finally {
      this.setData({ submitting: false })
    }
  },

  goEntry() {
    const tableId = this.data.tableId || wx.getStorageSync('currentTableId') || ''
    const url = tableId ? `/pages/entry/index?tableId=${tableId}` : '/pages/entry/index'
    wx.redirectTo({ url })
  },

  goStatus() {
    wx.redirectTo({
      url: `/pages/status/index?tableId=${this.data.tableId}`
    })
  },

  goMine() {
    wx.redirectTo({
      url: `/pages/mine/index?tableId=${this.data.tableId}`
    })
  }
})
