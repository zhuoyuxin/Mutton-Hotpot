function formatPrice(value) {
  const price = Number(value || 0)
  if (Number.isNaN(price)) {
    return '0.00'
  }
  return price.toFixed(2)
}

function maskPhone(phone) {
  if (!phone) {
    return ''
  }
  return String(phone).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  if (value instanceof Date) {
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    const hours = String(value.getHours()).padStart(2, '0')
    const minutes = String(value.getMinutes()).padStart(2, '0')
    const seconds = String(value.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  }
  return String(value)
    .replace('T', ' ')
    .replace(/\.\d+Z?$/, '')
}

module.exports = {
  formatPrice,
  maskPhone,
  formatDateTime
}
