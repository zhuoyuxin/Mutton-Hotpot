import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

let isRedirecting401 = false

// 商户端 axios 实例
const merchantRequest = axios.create({
  baseURL: '',
  withCredentials: true,
  timeout: 15000
})

if (import.meta.env.DEV) {
  merchantRequest.interceptors.request.use(config => {
    console.log(`[merchant] ${config.method?.toUpperCase()} ${config.url}`)
    return config
  })
}

merchantRequest.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        if (!isRedirecting401) {
          isRedirecting401 = true
          sessionStorage.removeItem('merchantUser')
          router.push('/m/login')
          setTimeout(() => { isRedirecting401 = false }, 3000)
        }
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      if (!isRedirecting401) {
        isRedirecting401 = true
        sessionStorage.removeItem('merchantUser')
        router.push('/m/login')
        setTimeout(() => { isRedirecting401 = false }, 3000)
      }
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 用户端 axios 实例
const customerRequest = axios.create({
  baseURL: '',
  timeout: 15000
})

if (import.meta.env.DEV) {
  customerRequest.interceptors.request.use(config => {
    console.log(`[customer] ${config.method?.toUpperCase()} ${config.url}`)
    return config
  })
}

// 自动附加 X-Phone header
customerRequest.interceptors.request.use(config => {
  const phone = localStorage.getItem('customerPhone')
  if (phone) {
    config.headers['X-Phone'] = phone
  }
  return config
})

customerRequest.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 40101) {
      // 用户端需要手机号
      const tableId = localStorage.getItem('currentTableId')
      router.push('/c/login/' + (tableId || '0'))
      return Promise.reject(new Error('请先登录'))
    }
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export { merchantRequest, customerRequest }
