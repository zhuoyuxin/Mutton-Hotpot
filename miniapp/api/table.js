const { request } = require('../utils/request')

function getTableDetail(tableId) {
  return request({
    url: `/api/c/table/${tableId}`
  })
}

module.exports = {
  getTableDetail
}
