/**
 * 订单状态映射工具
 * order status: 0-待确认 1-制作中 2-部分上菜 3-全部上菜 4-已结账 5-已取消
 * item status: 0-待确认 1-待上菜 2-已上菜 3-库存不足/已退菜
 */

export const orderStatusText = (s) =>
  ['待确认', '制作中', '部分上菜', '全部上菜', '已结账', '已取消'][s] || ''

export const orderStatusType = (s) =>
  ['info', '', 'warning', 'success', '', 'danger'][s] || ''

export const itemStatusText = (s) =>
  ['待确认', '待上菜', '已上菜', '库存不足', '已退菜'][s] || ''

export const itemStatusType = (s) =>
  ['info', '', 'success', 'warning', 'danger'][s] || ''
