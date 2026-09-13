import http from '@/utils/request'

export const authApi = {
  login: (data) => http.post('/api/admin/auth/login', data),
  info: () => http.get('/api/admin/auth/info'),
  logout: () => http.post('/api/admin/auth/logout'),
  password: (data) => http.put('/api/admin/auth/password', data)
}

export const userApi = {
  list: (params) => http.get('/api/admin/users', { params }),
  detail: (id) => http.get(`/api/admin/users/${id}`),
  update: (id, data) => http.put(`/api/admin/users/${id}`, data),
  recharge: (id, data, key) => http.post(`/api/admin/users/${id}/recharge`, data, { headers: { 'Idempotency-Key': key } }),
  adjustPoints: (id, data, key) => http.post(`/api/admin/users/${id}/points`, data, { headers: { 'Idempotency-Key': key } }),
  hourLogs: (id) => http.get(`/api/admin/users/${id}/hour-logs`),
  pointLogs: (id) => http.get(`/api/admin/users/${id}/point-logs`)
}

export const courseApi = {
  danceTypes: () => http.get('/api/admin/dance-types'),
  saveDanceType: (d) => d.id ? http.put(`/api/admin/dance-types/${d.id}`, d) : http.post('/api/admin/dance-types', d),
  delDanceType: (id) => http.delete(`/api/admin/dance-types/${id}`),
  levels: () => http.get('/api/admin/levels'),
  saveLevel: (d) => d.id ? http.put(`/api/admin/levels/${d.id}`, d) : http.post('/api/admin/levels', d),
  delLevel: (id) => http.delete(`/api/admin/levels/${id}`),
  rooms: () => http.get('/api/admin/rooms'),
  saveRoom: (d) => d.id ? http.put(`/api/admin/rooms/${d.id}`, d) : http.post('/api/admin/rooms', d),
  delRoom: (id) => http.delete(`/api/admin/rooms/${id}`),
  courses: (params) => http.get('/api/admin/courses', { params }),
  saveCourse: (d) => d.id ? http.put(`/api/admin/courses/${d.id}`, d) : http.post('/api/admin/courses', d),
  delCourse: (id) => http.delete(`/api/admin/courses/${id}`)
}

export const teacherApi = {
  list: () => http.get('/api/admin/teachers'),
  save: (d) => d.id ? http.put(`/api/admin/teachers/${d.id}`, d) : http.post('/api/admin/teachers', d),
  del: (id) => http.delete(`/api/admin/teachers/${id}`)
}

export const noticeApi = {
  list: (onlyEnabled) => http.get('/api/admin/notices', { params: { onlyEnabled } }),
  save: (d) => d.id ? http.put(`/api/admin/notices/${d.id}`, d) : http.post('/api/admin/notices', d),
  del: (id) => http.delete(`/api/admin/notices/${id}`)
}

export const scheduleApi = {
  list: (params) => http.get('/api/admin/schedules', { params }),
  save: (d) => d.id ? http.put(`/api/admin/schedules/${d.id}`, d) : http.post('/api/admin/schedules', d),
  cancel: (id) => http.post(`/api/admin/schedules/${id}/cancel`, {})
}

export const bookingApi = {
  bySchedule: (scheduleId) => http.get('/api/admin/bookings', { params: { scheduleId } }),
  book: (data) => http.post('/api/admin/bookings', data),
  cancel: (id) => http.post(`/api/admin/bookings/${id}/cancel`, {})
}

export const checkinApi = {
  code: (scheduleId) => http.post('/api/admin/checkin-codes', { scheduleId }),
  records: (params) => http.get('/api/admin/checkins', { params }),
  manual: (data) => http.post('/api/admin/checkins', data),
  attendance: (params) => http.get('/api/admin/reports/attendance', { params })
}

export const goodsApi = {
  list: (params) => http.get('/api/admin/goods', { params }),
  save: (d) => d.id ? http.put(`/api/admin/goods/${d.id}`, d) : http.post('/api/admin/goods', d),
  del: (id) => http.delete(`/api/admin/goods/${id}`)
}

export const redeemApi = {
  list: (params) => http.get('/api/admin/redeem-orders', { params }),
  approve: (id) => http.post(`/api/admin/redeem-orders/${id}/approve`, {}),
  reject: (id, reason) => http.post(`/api/admin/redeem-orders/${id}/reject`, { reason }),
  logistics: (id, data) => http.put(`/api/admin/redeem-orders/${id}/logistics`, data),
  complete: (id, pickupCode) => http.post(`/api/admin/redeem-orders/${id}/complete`, { pickupCode })
}

export const dashboardApi = {
  overview: (params) => http.get('/api/admin/dashboard/overview', { params }),
  alerts: (params) => http.get('/api/admin/dashboard/alerts', { params })
}

export const configApi = {
  list: () => http.get('/api/admin/configs'),
  update: (data) => http.put('/api/admin/configs', data)
}

export const uploadUrl = '/api/admin/upload'

export const videoApi = {
  publicList: () => http.get('/api/admin/public-videos'),
  publish: (data) => http.post('/api/admin/public-videos', data),
  hidePublic: (id) => http.delete(`/api/admin/public-videos/${id}`),
  list: (params) => http.get('/api/admin/videos', { params }),
  create: (data) => http.post('/api/admin/videos', data),
  del: (id) => http.delete(`/api/admin/videos/${id}`)
}

export const qaApi = {
  questions: (params) => http.get('/api/admin/qa/questions', { params }),
  answers: (questionId) => http.get(`/api/admin/qa/questions/${questionId}/answers`),
  hideQuestion: (id) => http.post(`/api/admin/qa/questions/${id}/hide`, {}),
  showQuestion: (id) => http.post(`/api/admin/qa/questions/${id}/show`, {}),
  hideAnswer: (id) => http.post(`/api/admin/qa/answers/${id}/hide`, {}),
  teacherAnswer: (questionId, data) => http.post(`/api/admin/qa/questions/${questionId}/answers`, data)
}

export const bannerApi = {
  list: () => http.get('/api/admin/banners'),
  save: (d) => d.id ? http.put(`/api/admin/banners/${d.id}`, d) : http.post('/api/admin/banners', d),
  del: (id) => http.delete(`/api/admin/banners/${id}`)
}
