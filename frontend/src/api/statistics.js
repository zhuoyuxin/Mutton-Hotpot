import { merchantRequest as req } from './request'

export const revenueTrend = (params) => req.get('/api/m/statistics/revenue-trend', { params })
export const topDishes = (params) => req.get('/api/m/statistics/top-dishes', { params })
export const hourlyDistribution = (params) => req.get('/api/m/statistics/hourly', { params })
