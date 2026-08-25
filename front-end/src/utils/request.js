import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

service.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code === 0) return body.data
    if (body.code === 1002) {
      localStorage.removeItem('admin_token')
      router.push('/login')
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(body)
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('admin_token')
      router.push('/login')
    }
    ElMessage.error('网络异常')
    return Promise.reject(err)
  }
)

export default service
