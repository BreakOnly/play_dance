import { http, silentLogin } from '@/utils/request'
import { BASE_URL } from '@/utils/config'
import { getToken, clearToken } from '@/utils/auth'

/** 相对路径补全为后端文件地址 */
export function fileUrl(u) {
  if (!u) return ''
  return /^https?:/.test(u) ? u : BASE_URL + u
}

export const api = {
  studio: () => http.get('/api/public/studio'),
  // 鉴权与资料
  login: (code) => http.post('/api/mp/auth/login', { code }),
  bindPhone: (code) => http.post('/api/mp/auth/phone', { code }),
  profile: () => http.get('/api/mp/profile'),
  updateProfile: (data) => http.put('/api/mp/profile', data),

  // 会员资料（参考代码风格）
  member: () => http.get('/api/mp/member'),
  updateMemberName: (name) => http.put('/api/mp/member', { name }),

  // 首页（聚合：账户/banner/体验课/指南/视频推荐/今日课/提醒/未读/会员有效期）
  home: () => http.get('/api/mp/home'),

  // 课表与约课（跨门店 + 关键字/舞种/难度/门店筛选）
  schedules: (params) => http.get('/api/mp/schedules', params),
  scheduleDetail: (id) => http.get('/api/mp/schedules/' + id),
  scheduleFilters: () => http.get('/api/mp/schedules/filter-options'),
  book: (scheduleId) => http.post('/api/mp/bookings', { scheduleId }),
  myBookings: (status) => http.get('/api/mp/bookings', status ? { status } : {}),
  cancelBooking: (id) => http.post('/api/mp/bookings/' + id + '/cancel', {}),

  // 业务配置（取消规则/注意事项/客服电话等）+ 门店列表
  getConfig: () => http.get('/api/mp/config'),
  stores: () => http.get('/api/mp/config/stores'),

  // 课时/积分流水
  hourLogs: () => http.get('/api/mp/account/hour-logs'),
  pointLogs: () => http.get('/api/mp/account/point-logs'),

  // 消息
  notifications: () => http.get('/api/mp/notifications'),
  unreadCount: () => http.get('/api/mp/notifications/unread'),
  readNotice: (id) => http.put('/api/mp/notifications/read', id ? { id } : {}),

  // 问答
  qaQuestions: (page, size) => http.get('/api/mp/qa/questions', { page, size }),
  qaDetail: (id) => http.get('/api/mp/qa/questions/' + id),
  qaAsk: (content, images) => http.post('/api/mp/qa/questions', { content, images }),
  qaAnswer: (id, content) => http.post('/api/mp/qa/questions/' + id + '/answers', { content }),
  qaLike: (targetType, targetId, liked) => http.post('/api/mp/qa/like', { targetType, targetId, liked }),

  // 视频（管理员上传，用户查看+点赞）
  videoRanking: (limit) => http.get('/api/mp/videos/ranking', limit ? { limit } : {}),
  videoRecommended: (limit) => http.get('/api/mp/videos/recommended', limit ? { limit } : {}),
  videoLike: (id, liked) => http.post('/api/mp/videos/' + id + '/like', { liked }),
  scanCheckin: (scene) => http.post('/api/mp/checkins/scan', { scene }),
  videoPlayUrl: (id) => http.get('/api/mp/user-videos/' + id + '/play'),

  // 手办兑换
  goods: (category) => http.get('/api/mp/goods', category ? { category } : {}),
  goodsDetail: (id) => http.get('/api/mp/goods/' + id),
  redeem: (goodsId, operationKey) => http.post('/api/mp/redeem', { goodsId }, { operationKey }),
  redeemOrders: (status) => http.get('/api/mp/redeem-orders', status ? { status } : {}),
  fillAddress: (id, name, phone, address) => http.put('/api/mp/redeem-orders/' + id + '/address', { name, phone, address })
}

/** 上传文件（视频/图片），返回服务器相对路径。token 过期时静默重登一次并重试 */
export function uploadFile(filePath, kind, _retried) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/api/mp/upload/' + (kind === 'video' ? 'video' : 'image'),
      filePath,
      name: 'file',
      header: { Authorization: 'Bearer ' + getToken() },
      success(res) {
        try {
          const body = JSON.parse(res.data)
          if (res.statusCode === 401 || body.code === 1002) {
            if (!_retried) {
              silentLogin().then(() => {
                uploadFile(filePath, kind, true).then(resolve).catch(reject)
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
          if (body.code === 0) resolve(body.data.url)
          else { uni.showToast({ title: body.message || '上传失败', icon: 'none' }); reject(body) }
        } catch (e) { reject(e) }
      },
      fail(err) {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}
