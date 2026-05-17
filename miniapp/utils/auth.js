const { wechatLogin } = require('../api/customer')

function clearCustomerSession() {
  wx.removeStorageSync('customerToken')
  wx.removeStorageSync('customerTokenExpiresAt')
  wx.removeStorageSync('customerProfile')
}

function getCustomerProfile() {
  return wx.getStorageSync('customerProfile') || null
}

async function ensureCustomerLogin(forceRefresh) {
  const force = !!forceRefresh
  const cachedToken = wx.getStorageSync('customerToken')
  const cachedExpiresAt = Number(wx.getStorageSync('customerTokenExpiresAt') || 0)
  const now = Math.floor(Date.now() / 1000)

  if (!force && cachedToken && cachedExpiresAt > now + 60) {
    return {
      token: cachedToken,
      expiresAt: cachedExpiresAt,
      customer: getCustomerProfile()
    }
  }

  const loginResult = await new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (!res.code) {
          reject(new Error('wx.login 未返回 code'))
          return
        }
        resolve(res)
      },
      fail(error) {
        reject(error)
      }
    })
  })

  const authData = await wechatLogin({ code: loginResult.code })
  wx.setStorageSync('customerToken', authData.token)
  wx.setStorageSync('customerTokenExpiresAt', authData.expiresAt)
  wx.setStorageSync('customerProfile', authData.customer || {})
  return authData
}

module.exports = {
  clearCustomerSession,
  ensureCustomerLogin,
  getCustomerProfile
}
