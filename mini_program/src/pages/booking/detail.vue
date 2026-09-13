<template>
  <view class="page">
    <scroll-view scroll-y class="body">
      <!-- 媒体区：展示首位 banner 图（后台「Banner 管理」可换），无 banner 时回退占位 -->
      <view class="media">
        <image v-if="mediaImg" class="media-img" :src="mediaImg" mode="aspectFill" />
        <view v-else class="media-ph">
          <text class="mp-dance">{{ detail.danceTypeName || 'DANCE' }}</text>
        </view>
      </view>

      <!-- 老师行 -->
      <view class="teacher-row">
        <image v-if="detail.teacherAvatar" :src="fileUrl(detail.teacherAvatar)" class="t-avatar" mode="aspectFill" />
        <view v-else class="t-avatar ph">{{ (detail.teacherName || 'T')[0] }}</view>
        <view class="t-grow">
          <view class="t-name">{{ detail.teacherName || '老师' }}</view>
          <view class="t-role">{{ detail.danceTypeName || '' }} 导师</view>
        </view>
      </view>

      <!-- 课程信息块 -->
      <view class="info">
        <view class="i-title">{{ detail.courseName }}</view>
        <view class="i-stars">
          <text class="star" v-for="i in 5" :key="i" :class="{ filled: i <= starCount }">★</text>
        </view>
        <view class="i-line">{{ dateCn(detail.startTime) }} {{ fmtRange(detail.startTime, detail.endTime) }}</view>
        <view class="i-line">{{ detail.room ? detail.room + ' 教室' : '待定' }}</view>
        <view class="i-line">🎟 剩余 {{ remaining }} 席</view>
        <view v-if="detail.song" class="i-line">🎵 {{ detail.song }}</view>
      </view>

      <!-- 签到提示条 -->
      <view class="checkin-tip">
        <view class="ct-line1">到店后用微信扫一扫门店签到码</view>
        <view class="ct-line2">签到成功自动赠送 {{ pointsPerCheckin }} 积分，可在手办商城兑换</view>
      </view>

      <!-- 课程内容分区：5 张信息卡 -->
      <view class="sec-card card">
        <view class="sc-h"><text class="sc-t">课程简介</text></view>
        <text class="sc-b">{{ detail.description || '暂无简介' }}</text>
      </view>
      <view class="sec-card card">
        <view class="sc-h"><text class="sc-t">教师简介</text></view>
        <text class="sc-b">{{ detail.teacherIntro || '暂无简介' }}</text>
      </view>
      <view class="sec-card card">
        <view class="sc-h"><text class="sc-t">舞种介绍</text></view>
        <text class="sc-b">{{ detail.danceTypeIntro || `${detail.danceTypeName || '舞种'}课程，欢迎体验。` }}</text>
      </view>
      <view class="sec-card card">
        <view class="sc-h"><text class="sc-t">支付规则</text></view>
        <view class="sc-list">
          <text class="sc-li">· 约课冻结 {{ detail.costHours || 1 }} 课时</text>
          <text class="sc-li">· 距开课至少 {{ cancelHours }} 小时可在线取消</text>
          <text class="sc-li">· 临近开课请联系管理员取消；爽约核销冻结课时</text>
          <text class="sc-li">· 候补不冻结课时；取消时已到期的原课时不会恢复</text>
        </view>
      </view>
      <view class="sec-card card">
        <view class="sc-h"><text class="sc-t">💡 注意事项</text></view>
        <text class="sc-b pre">{{ detail.noticeContent || courseNotice || '请提前到店，遵守课堂秩序。' }}</text>
      </view>

      <view style="height: 160rpx;"></view>
    </scroll-view>

    <!-- 底部约课 CTA：已约课展示「取消预约」，6 小时内不可线上取消 -->
    <view class="cta-bar">
      <button class="btn-primary cta-btn"
              :class="{ disabled: ctaDisabled, 'cta-cancel': myStatus === 'booked' || myStatus === 'waitlist' }"
              @tap="onCta">
        {{ ctaText }}
      </button>
    </view>

    <!-- 约课确认弹层 -->
    <view v-if="bookPopup" class="mask" @tap="bookPopup = false">
      <view class="sheet card" @tap.stop>
        <view class="s-title">确定预约课程？</view>
        <view class="s-text">{{ detail.courseName }} · {{ fmtRange(detail.startTime, detail.endTime) }}，{{ remaining === 0 ? '候补暂不冻结，转正时冻结' : '将冻结' }} {{ detail.costHours || 1 }} 课时；距开课至少 {{ cancelHours }} 小时可在线取消。</view>
        <view class="s-btns">
          <button class="s-cancel" @tap="bookPopup = false">取 消</button>
          <button class="btn-primary s-ok" @tap="doBook">确 定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api, fileUrl } from '@/api'

const detail = ref({})
const cancelHours = ref(6)
const pointsPerCheckin = ref(100)
const courseNotice = ref('')
const bookPopup = ref(false)
const scheduleId = ref(null)
/** 媒体区图：取首位 banner（后台可换），无则回退占位 */
const mediaImg = ref('')

onShow(() => {
  api.scheduleDetail(scheduleId.value).then((d) => { detail.value = d || {} }).catch(() => {})
  api.getConfig().then((cfg) => {
    cancelHours.value = cfg.cancelFreeHours ?? 6
    pointsPerCheckin.value = cfg.pointsPerCheckin ?? 100
    courseNotice.value = cfg.courseNotice || ''
  }).catch(() => {})
  api.home().then((d) => {
    const b = (d.banners || [])[0]
    if (b && b.imageUrl) mediaImg.value = fileUrl(b.imageUrl)
  }).catch(() => {})
})

const myStatus = computed(() => detail.value.myBookingStatus)
const remaining = computed(() => Math.max(0, (detail.value.capacity || 0) - (detail.value.bookedCount || 0)))

/** 课次是否已不可约：返回状态文案（已取消/已结束/已开始），空串=仍可预约 */
function courseState(s) {
  if (!s || !s.startTime) return ''
  if (s.status === 'cancelled') return '已取消'
  if (s.status === 'finished') return '已结束'
  const now = new Date()
  if (s.endTime && new Date(s.endTime.replace(/-/g, '/').replace('T', ' ')) <= now) return '已结束'
  if (new Date(s.startTime.replace(/-/g, '/').replace('T', ' ')) <= now) return '已开始'
  return ''
}
const closed = computed(() => !!courseState(detail.value))

/** 底部按钮文案与可点状态 */
const ctaText = computed(() => {
  if (myStatus.value === 'booked') return '取消预约'
  if (myStatus.value === 'waitlist') return '取消候补'
  if (myStatus.value === 'checked') return '已签到'
  if (closed.value) return courseState(detail.value)
  return remaining.value === 0 ? '加入候补' : '立即约课'
})
const ctaDisabled = computed(() => myStatus.value === 'checked' || (!myStatus.value && closed.value))

/** 距开课还剩几小时 */
function hoursUntilStart() {
  const t = detail.value.startTime
  if (!t) return 0
  return (new Date(t.replace(/-/g, '/').replace('T', ' ')) - new Date()) / 3600000
}

/** 星级：取后台难度等级的sort(1-5)，未配置时默认3星 */
const starCount = computed(() => {
  const s = detail.value.levelSort
  if (s && s >= 1 && s <= 5) return s
  return 3
})

onLoad((opts) => { scheduleId.value = opts.id })

/** 底部按钮统一入口：已约 → 取消流程；未约 → 约课流程 */
function onCta() {
  if (myStatus.value === 'booked') { onCancelBooking(); return }
  if (myStatus.value === 'waitlist') { onCancelWaitlist(); return }
  if (myStatus.value === 'checked') return
  if (closed.value) {
    const st = courseState(detail.value)
    uni.showToast({ title: st === '已结束' ? '课程已结束' : '该课' + st + '，无法预约', icon: 'none' })
    return
  }
  bookPopup.value = true
}

/** 已约课：6 小时内不可线上取消，之外弹确认取消 */
function onCancelBooking() {
  if (hoursUntilStart() < cancelHours.value) {
    uni.showModal({
      title: '无法取消预约',
      content: `距开课已不足 ${cancelHours.value} 小时，本节课暂不支持在线取消。如确有特殊情况，请联系门店工作人员协助处理。`,
      showCancel: false,
      confirmColor: '#ff2d9b'
    })
    return
  }
  const d = detail.value
  uni.showModal({
    title: '取消预约',
    content: `确定要取消预约该课程吗？\n《${d.courseName}》 ${dateCn(d.startTime)} ${fmtRange(d.startTime, d.endTime)}\n取消后冻结的 ${d.costHours || 1} 课时将立即退回。`,
    confirmText: '取消预约',
    cancelText: '再想想',
    confirmColor: '#ff2d9b',
    success: (r) => {
      if (!r.confirm) return
      api.cancelBooking(d.myBookingId).then(() => {
        uni.showToast({ title: '已取消，课时已退回', icon: 'none' })
        api.scheduleDetail(scheduleId.value).then((x) => { detail.value = x || {} })
      }).catch(() => {})
    }
  })
}

/** 候补单取消（不受 6 小时限制，不涉及课时冻结） */
function onCancelWaitlist() {
  const d = detail.value
  uni.showModal({
    title: '取消候补',
    content: `确定取消《${d.courseName}》的候补吗？取消后当前候补位置将立即释放。`,
    confirmText: '取消候补',
    cancelText: '再想想',
    confirmColor: '#ff2d9b',
    success: (r) => {
      if (!r.confirm) return
      api.cancelBooking(d.myBookingId).then(() => {
        uni.showToast({ title: '已取消候补', icon: 'none' })
        api.scheduleDetail(scheduleId.value).then((x) => { detail.value = x || {} })
      }).catch(() => {})
    }
  })
}

function doBook() {
  api.book(scheduleId.value).then((bk) => {
    bookPopup.value = false
    uni.showToast({ title: bk.status === 'waitlist' ? '已加入候补' : '约课成功', icon: 'none' })
    api.scheduleDetail(scheduleId.value).then((d) => { detail.value = d || {} })
    setTimeout(() => uni.navigateTo({ url: '/pages/mybookings/mybookings' }), 1000)
  }).catch(() => {})
}

function dateCn(t) {
  if (!t) return ''
  const d = new Date(t.replace(/-/g, '/').replace('T', ' '))
  return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日'
}
function fmtRange(s, e) {
  if (!s) return ''
  return s.substring(11, 16) + ' - ' + (e ? e.substring(11, 16) : '')
}
</script>

<style lang="scss" scoped>
.page { height: 100vh; display: flex; flex-direction: column; background: $bg; }
.body { flex: 1; }
.media { height: 380rpx; position: relative; }
.media-img { width: 100%; height: 100%; }
.media-ph { height: 100%; background: linear-gradient(140deg, $pink-dk, #2a0a1d 60%, #160a12); display: flex; align-items: center; justify-content: center; }
.mp-dance { font-size: 56rpx; font-weight: 900; letter-spacing: 8rpx; color: rgba(255,255,255,.85); }
.teacher-row { display: flex; align-items: center; gap: 20rpx; padding: 24rpx 30rpx 8rpx; }
.t-avatar { width: 96rpx; height: 96rpx; border-radius: 50%; border: 3rpx solid $pink; }
.t-avatar.ph { display: flex; align-items: center; justify-content: center; font-size: 44rpx; font-weight: 900; background: $card2; color: $pink2; }
.t-name { font-size: 32rpx; font-weight: 800; }
.t-role { color: $txt2; font-size: 24rpx; margin-top: 6rpx; }
.info { padding: 16rpx 30rpx; }
.i-title { font-size: 38rpx; font-weight: 900; }
.i-stars { margin-top: 12rpx; }
.star { color: #3a3a42; font-size: 28rpx; margin-right: 6rpx; }
.star.filled { color: $pink; }
.i-line { color: $txt2; font-size: 25rpx; margin-top: 14rpx; }
.checkin-tip { margin: 26rpx 30rpx 0; background: linear-gradient(90deg, rgba(255,45,155,.22), rgba(255,45,155,.06)); border: 1rpx solid rgba(255,45,155,.35); border-radius: 18rpx; padding: 22rpx 26rpx; }
.ct-line1 { font-size: 27rpx; font-weight: 700; }
.ct-line2 { color: $txt2; font-size: 23rpx; margin-top: 8rpx; }
.sec-card { margin: 22rpx 30rpx 0; padding: 24rpx 26rpx; }
.sc-h { display: flex; align-items: center; gap: 12rpx; }
.sc-t { font-size: 28rpx; font-weight: 800; border-left: 6rpx solid $pink; padding-left: 14rpx; }
.sc-b { display: block; color: $txt2; font-size: 25rpx; line-height: 1.8; margin-top: 14rpx; }
/* 注意事项等内容支持换行展示 */
.sc-b.pre { white-space: pre-line; }
.sc-list { display: flex; flex-direction: column; gap: 8rpx; margin-top: 14rpx; }
.sc-li { color: $txt2; font-size: 25rpx; line-height: 1.7; }
.cta-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 30rpx calc(env(safe-area-inset-bottom) + 20rpx); background: linear-gradient(180deg, transparent, $bg 35%); }
.cta-btn { font-size: 32rpx; font-weight: 800; padding: 8rpx 0; }
.cta-btn.disabled { opacity: .5; }
/* 取消预约按钮：粉描边空心，与「立即约课」主按钮区分，避免误触 */
.cta-btn.cta-cancel { background: transparent; border: 2rpx solid $pink; color: $pink2; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.65); z-index: 1000; display: flex; align-items: flex-end; }
.sheet { width: 100%; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 32rpx calc(env(safe-area-inset-bottom) + 32rpx); }
.s-title { font-size: 32rpx; font-weight: 800; text-align: center; }
.s-text { color: $txt2; font-size: 26rpx; line-height: 1.7; margin: 24rpx 0 32rpx; }
.s-btns { display: flex; gap: 20rpx; }
.s-cancel { flex: 1; background: $card2; color: $txt2; font-size: 28rpx; border-radius: 18rpx; }
.s-ok { flex: 1; font-size: 28rpx; }
</style>
