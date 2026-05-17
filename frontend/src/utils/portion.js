export const PORTION_FULL = 'FULL'
export const PORTION_HALF = 'HALF'

export const normalizePortionType = (value) => {
  const normalized = String(value || PORTION_FULL).trim().toUpperCase()
  return normalized === PORTION_HALF ? PORTION_HALF : PORTION_FULL
}

export const supportsHalfPortion = (dish) => (
  Number(dish?.allowHalfPortion || 0) === 1 && Number(dish?.halfPrice || 0) > 0
)

export const getPortionPrice = (dish, portionType) => (
  normalizePortionType(portionType) === PORTION_HALF && supportsHalfPortion(dish)
    ? Number(dish?.halfPrice || 0)
    : Number(dish?.price || 0)
)

export const getPortionLabel = (portionType) => (
  normalizePortionType(portionType) === PORTION_HALF ? '半份' : '整份'
)

export const buildDishDisplayName = (dishName, portionType) => (
  `${dishName || ''}${normalizePortionType(portionType) === PORTION_HALF ? '（半份）' : ''}`
)

export const createCartKey = (dishId, portionType) => (
  `${Number(dishId)}:${normalizePortionType(portionType)}`
)

export const parseCartKey = (key) => {
  const [dishIdText, portionText] = String(key || '').split(':')
  return {
    dishId: Number(dishIdText || 0),
    portionType: normalizePortionType(portionText)
  }
}
