import { merchantRequest as req } from './request'

export const list = () => req.get('/api/m/table/list')
export const add = (data) => req.post('/api/m/table/add', data)
export const update = (data) => req.put('/api/m/table/update', data)
export const remove = (id) => req.delete(`/api/m/table/${id}`)
export const qrcode = (id) => req.get(`/api/m/table/qrcode/${id}`)
