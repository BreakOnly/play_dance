const TOKEN_KEY = 'mp_token'
const USER_KEY = 'mp_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}
export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}
export function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
export function setUser(user) {
  uni.setStorageSync(USER_KEY, user)
}
export function getUser() {
  return uni.getStorageSync(USER_KEY) || {}
}
export function isLogin() {
  return !!getToken()
}
