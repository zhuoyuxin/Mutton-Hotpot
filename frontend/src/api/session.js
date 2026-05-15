import { merchantRequest as req } from './request'

export const current = (tableId) => req.get(`/api/m/session/current/${tableId}`)
export const detail = (id) => req.get(`/api/m/session/detail/${id}`)
export const checkout = (id, data) => req.put(`/api/m/session/checkout/${id}`, data)
export const history = (params) => req.get('/api/m/session/history', { params })
