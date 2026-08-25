import { BASE_URL } from './config'
import { getToken, clearToken } from './auth'

/**
 * 统一请求封装：自动带 token，解析后端 Result{code,message,data}。
 * 成功(code=0) resolve(data)；失败 reject 并 toast；未登录(1002) 跳登录。
 */
export function request(options) {
  const { url, method = 'GET', data = {}, showLoading = false, silent = false } = options
  return new Promise((resolve, reject) => {
    if (showLoading) uni.showLoading({ title: '加载中', mask: true })
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        Authorization: getToken() ? 'Bearer ' + getToken() : ''
      },
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401 || body.code === 1002) {
          clearToken()
          uni.reLaunch({ url: '/pages/login/login' })
          reject(body)
          return
        }
        if (body.code === 0) {
          resolve(body.data)
        } else {
          if (!silent) uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        if (!silent) uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      },
      complete() {
        if (showLoading) uni.hideLoading()
      }
    })
  })
}

export const http = {
  get: (url, data, opt) => request({ url, method: 'GET', data, ...opt }),
  post: (url, data, opt) => request({ url, method: 'POST', data, ...opt }),
  put: (url, data, opt) => request({ url, method: 'PUT', data, ...opt })
}
