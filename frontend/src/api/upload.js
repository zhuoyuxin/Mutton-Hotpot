import { merchantRequest as req } from './request'

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return req.post('/api/m/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
