/**
 * 价格格式化：分 -> 元字符串
 */
export const formatPrice = (cents) => (cents / 100).toFixed(2)
