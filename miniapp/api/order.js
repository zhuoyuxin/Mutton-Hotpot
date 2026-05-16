const { request } = require('../utils/request')

function createCustomerOrder(data) {
  return request({
    url: '/api/c/order/create',
    method: 'POST',
    data
  })
}

function getTableOrders(tableId) {
  return request({
    url: `/api/c/order/table/${tableId}`
  })
}

module.exports = {
  createCustomerOrder,
  getTableOrders
}
