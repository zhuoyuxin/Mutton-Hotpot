const { request } = require('../utils/request')

function customerLogin(data) {
  return request({
    url: '/api/c/auth/login',
    method: 'POST',
    data
  })
}

function customerInfo() {
  return request({
    url: '/api/c/customer/info',
    redirectOn401: false,
    showError: false
  })
}

function customerOrders() {
  return request({
    url: '/api/c/customer/orders',
    redirectOn401: false,
    showError: false
  })
}

function customerPoints() {
  return request({
    url: '/api/c/customer/points',
    redirectOn401: false,
    showError: false
  })
}

module.exports = {
  customerLogin,
  customerInfo,
  customerOrders,
  customerPoints
}
