const PORTION_FULL = 'FULL'
const PORTION_HALF = 'HALF'

function normalizePortionType(value) {
  const normalized = String(value || PORTION_FULL).trim().toUpperCase()
  return normalized === PORTION_HALF ? PORTION_HALF : PORTION_FULL
}

function supportsHalfPortion(dish) {
  return Number(dish && dish.allowHalfPortion ? dish.allowHalfPortion : 0) === 1
    && Number(dish && dish.halfPrice ? dish.halfPrice : 0) > 0
}

function getPortionPrice(dish, portionType) {
  if (normalizePortionType(portionType) === PORTION_HALF && supportsHalfPortion(dish)) {
    return Number(dish && dish.halfPrice ? dish.halfPrice : 0)
  }
  return Number(dish && dish.price ? dish.price : 0)
}

function getPortionLabel(portionType) {
  return normalizePortionType(portionType) === PORTION_HALF ? '半份' : '整份'
}

function buildDishDisplayName(dishName, portionType) {
  return `${dishName || ''}${normalizePortionType(portionType) === PORTION_HALF ? '（半份）' : ''}`
}

function createCartKey(dishId, portionType) {
  return `${Number(dishId)}:${normalizePortionType(portionType)}`
}

function parseCartKey(key) {
  const parts = String(key || '').split(':')
  return {
    dishId: Number(parts[0] || 0),
    portionType: normalizePortionType(parts[1])
  }
}

module.exports = {
  PORTION_FULL,
  PORTION_HALF,
  normalizePortionType,
  supportsHalfPortion,
  getPortionPrice,
  getPortionLabel,
  buildDishDisplayName,
  createCartKey,
  parseCartKey
}
