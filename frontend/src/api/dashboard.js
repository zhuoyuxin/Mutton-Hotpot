import { merchantRequest as req } from './request'

export const getData = () => req.get('/api/m/dashboard')
