import { merchantRequest as mReq, customerRequest as cReq } from './request'

export const merchantList = (params) => mReq.get('/api/m/order/list', { params })
export const merchantCreate = (data) => mReq.post('/api/m/order/create', data)
export const confirm = (id) => mReq.put(`/api/m/order/confirm/${id}`)
export const serveItem = (id, data) => mReq.put(`/api/m/order/serve-item/${id}`, data)
export const cancelItem = (id) => mReq.put(`/api/m/order/cancel-item/${id}`)
export const cancelOrder = (id) => mReq.put(`/api/m/order/cancel/${id}`)
export const getItems = (orderId) => mReq.get(`/api/m/order/items/${orderId}`)

export const customerCreate = (data) => cReq.post('/api/c/order/create', data)
export const tableOrders = (tableId) => cReq.get(`/api/c/order/table/${tableId}`)
