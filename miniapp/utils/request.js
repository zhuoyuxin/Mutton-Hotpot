const { API_BASE_URL } = require('./config')

let redirecting401 = false

function showToast(title) {
  wx.showToast({
    title,
    icon: 'none',
    duration: 2200
  })
}

function jumpToEntry() {
  if (redirecting401) {
    return
  }
  redirecting401 = true
  const tableId = wx.getStorageSync('currentTableId')
  const url = tableId
    ? `/pages/entry/index?tableId=${tableId}`
    : '/pages/entry/index'

  wx.reLaunch({ url })
  setTimeout(() => {
    redirecting401 = false
  }, 1500)
}

function request(options) {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    showError = true,
    redirectOn401 = true
  } = options

  return new Promise((resolve, reject) => {
    const phone = wx.getStorageSync('customerPhone')
    const mergedHeader = {
      'Content-Type': 'application/json',
      ...header
    }

    if (phone) {
      mergedHeader['X-Phone'] = phone
    }

    wx.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: mergedHeader,
      success(res) {
        const payload = res.data || {}

        if (payload.code === 40101) {
          if (redirectOn401) {
            jumpToEntry()
          }
          reject(new Error(payload.message || 'Please login first'))
          return
        }

        if (typeof payload.code !== 'number') {
          if (showError) {
            showToast('接口返回格式异常')
          }
          reject(new Error('Unexpected response payload'))
          return
        }

        if (payload.code !== 0) {
          if (showError) {
            showToast(payload.message || '请求失败')
          }
          reject(new Error(payload.message || 'Request failed'))
          return
        }

        resolve(payload.data)
      },
      fail(error) {
        if (showError) {
          showToast('网络异常，请检查接口地址')
        }
        reject(error)
      }
    })
  })
}

module.exports = {
  request
}
