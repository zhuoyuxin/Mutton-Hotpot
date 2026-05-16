const { request } = require('../utils/request')

function getCustomerDishes() {
  return request({
    url: '/api/c/dish/list'
  })
}

module.exports = {
  getCustomerDishes
}
