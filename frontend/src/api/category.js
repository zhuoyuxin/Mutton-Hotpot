import { merchantRequest as req } from './request'

export const list = () => req.get('/api/m/category/list')
export const add = (data) => req.post('/api/m/category/add', data)
export const update = (data) => req.put('/api/m/category/update', data)
export const remove = (id) => req.delete(`/api/m/category/${id}`)
