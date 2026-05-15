import { merchantRequest as mReq, customerRequest as cReq } from './request'

export const merchantList = () => mReq.get('/api/m/dish/list')
export const addDish = (data) => mReq.post('/api/m/dish/add', data)
export const updateDish = (data) => mReq.put('/api/m/dish/update', data)
export const toggleDish = (id) => mReq.put(`/api/m/dish/toggle/${id}`)
export const updateStock = (data) => mReq.put('/api/m/dish/stock', data)
export const customerList = () => cReq.get('/api/c/dish/list')
