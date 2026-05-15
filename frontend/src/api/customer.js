import { merchantRequest as mReq, customerRequest as cReq } from './request'

export const merchantList = (params) => mReq.get('/api/m/customer/list', { params })
export const merchantDetail = (id) => mReq.get(`/api/m/customer/detail/${id}`)
export const manualPoints = (data) => mReq.post('/api/m/customer/points', data)

export const customerLogin = (data) => cReq.post('/api/c/auth/login', data)
export const customerInfo = () => cReq.get('/api/c/customer/info')
export const customerOrders = () => cReq.get('/api/c/customer/orders')
export const customerPoints = () => cReq.get('/api/c/customer/points')
