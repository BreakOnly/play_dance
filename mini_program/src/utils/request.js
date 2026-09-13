import { BASE_URL } from './config'
import { getToken, clearToken, setToken, setUser } from './auth'

/**
 * 统一请求封装：自动带 token，解析后端 Result{code,message,data}。
 * 成功(code=0) resolve(data)；失败 reject 并 toast。
 * token 缺失/过期(401/1002)：先静默重登一次并自动重放原请求，重登也失败才踢登录页。
 */
export function request(options) {
  options = { ...options, operationKey: options.operationKey || (Date.now().toString(36) + Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2)) }
  const { url, method = 'GET', data = {}, showLoading = false, silent = false } = options
  return new Promise((resolve, reject) => {
    if (showLoading) uni.showLoading({ title: '加载中', mask: true })
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Idempotency-Key': options.operationKey,
        Authorization: getToken() ? 'Bearer ' + getToken() : ''
      },
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401 || body.code === 1002) {
          if (!options._retried && url !== '/api/mp/auth/login' && getToken()) {
            silentLogin().then(() => {
              request({ ...options, _retried: true }).then(resolve).catch(reject)
            }).catch(() => {
              clearToken()
              uni.reLaunch({ url: '/pages/login/login' })
              reject(body)
            })
            return
          }
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

/**
 * 静默登录：微信一键授权拿 code 换 token 并缓存，全程无感。
 * 单飞锁：并发调用只真正登录一次，共享同一个 Promise。
 */
let silentLoginPromise = null
export function silentLogin() {
  if (silentLoginPromise) return silentLoginPromise
  silentLoginPromise = new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.login({
      provider: 'weixin',
      success: (res) => exchange(res.code, resolve, reject),
      fail: reject
    })
    // #endif
    // #ifndef MP-WEIXIN
    let devCode = uni.getStorageSync('local_dev_identity')
    if (!devCode) { devCode = 'local_' + Date.now().toString(36) + Math.random().toString(36).slice(2); uni.setStorageSync('local_dev_identity', devCode) }
    exchange(devCode, resolve, reject)
    // #endif
  }).finally(() => { silentLoginPromise = null })
  return silentLoginPromise
}

function exchange(code, resolve, reject) {
  http.post('/api/mp/auth/login', { code }, { silent: true }).then((data) => {
    setToken(data.token)
    setUser({ id: data.userId, name: data.name })
    resolve(data)
  }).catch(reject)
}

export const http = {
  get: (url, data, opt) => request({ url, method: 'GET', data, ...opt }),
  post: (url, data, opt) => request({ url, method: 'POST', data, ...opt }),
  put: (url, data, opt) => request({ url, method: 'PUT', data, ...opt })
}
