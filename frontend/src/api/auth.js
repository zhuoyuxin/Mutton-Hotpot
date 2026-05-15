import { merchantRequest as req } from './request'

export const login = (data) => req.post('/api/m/auth/login', data)
export const logout = () => req.post('/api/m/auth/logout')
export const changePassword = (data) => req.put('/api/m/auth/password', data)
export const getInfo = () => req.get('/api/m/auth/info')
