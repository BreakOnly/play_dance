<template>
  <view class="page">
    <scroll-view scroll-y class="body">
      <!-- 媒体区（课堂预览占位） -->
      <view class="media">
        <view class="media-ph">
          <text class="mp-dance">{{ detail.danceTypeName || 'DANCE' }}</text>
          <view class="mp-play">▶</view>
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
        <view class="i-tags">
          <text v-if="detail.levelName" class="tag pink-tag">{{ detail.levelName }}</text>
          <text class="tag">零基础</text>
          <text v-if="detail.courseType === 'private'" class="tag">1对1</text>
          <text class="tag">成品舞</text>
        </view>
        <view class="i-line">🕐 {{ fmtRange(detail.startTime, detail.endTime) }} {{ weekCn(detail.startTime) }}</view>
        <view class="i-line">📍 {{ detail.room || '' }} · {{ detail.storeName || storeName }}</view>
        <view class="i-line">🎟 剩余 {{ remaining }} 席</view>
        <view v-if="detail.song" class="i-line">🎵 {{ detail.song }}</view>
      </view>

      <!-- 签到提示条 -->
      <view class="checkin-tip">
        <view class="ct-line1">到店免扫码 · 工作人员后台确认签到</view>
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
          <text class="sc-li">· 开课前 {{ cancelHours }} 小时内取消免费退回</text>
          <text class="sc-li">· 逾期取消或爽约将扣除课时</text>
        </view>
      </view>
      <view class="sec-card card">
        <view class="sc-h"><text class="sc-t">注意事项</text></view>
        <text class="sc-b">{{ courseNotice || '请提前到店，遵守课堂秩序。' }}</text>
      </view>

      <view style="height: 160rpx;"></view>
    </scroll-view>

    <!-- 底部约课 CTA -->
    <view class="cta-bar">
      <button class="btn-primary cta-btn" :class="{ disabled: booked }" @tap="openBook">
        {{ booked ? '已约课' : '立即约课' }}
      </button>
    </view>

    <!-- 约课确认弹层 -->
    <view v-if="bookPopup" class="mask" @tap="bookPopup = false">
      <view class="sheet card" @tap.stop>
        <view class="s-title">确定预约课程？</view>
        <view class="s-text">{{ detail.courseName }} · {{ fmtRange(detail.startTime, detail.endTime) }}，将冻结 {{ detail.costHours || 1 }} 课时；开课前 {{ cancelHours }} 小时可免费取消。</view>
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
const storeName = ref('')
const cancelHours = ref(6)
const pointsPerCheckin = ref(100)
const courseNotice = ref('')
const bookPopup = ref(false)
const scheduleId = ref(null)

const booked = computed(() => ['booked', 'checked', 'waitlist'].includes(detail.value.myBookingStatus))
const remaining = computed(() => Math.max(0, (detail.value.capacity || 0) - (detail.value.bookedCount || 0)))

/** 星级：取后台难度等级的sort(1-5)，未配置时默认3星 */
const starCount = computed(() => {
  const s = detail.value.levelSort
  if (s && s >= 1 && s <= 5) return s
  return 3
})

onLoad((opts) => { scheduleId.value = opts.id })
onShow(() => {
  api.scheduleDetail(scheduleId.value).then((d) => { detail.value = d || {} }).catch(() => {})
  api.getConfig().then((cfg) => {
    storeName.value = cfg.storeName || ''
    cancelHours.value = cfg.cancelFreeHours || 6
    pointsPerCheckin.value = cfg.pointsPerCheckin || 100
    courseNotice.value = cfg.courseNotice || ''
  }).catch(() => {})
})

function openBook() {
  if (booked.value) {
    uni.showToast({ title: '你已约该课', icon: 'none' })
    return
  }
  bookPopup.value = true
}

function doBook() {
  api.book(scheduleId.value).then((bk) => {
    bookPopup.value = false
    uni.showToast({ title: bk.status === 'waitlist' ? '已加入候补' : '约课成功', icon: 'none' })
    api.scheduleDetail(scheduleId.value).then((d) => { detail.value = d || {} })
    setTimeout(() => uni.navigateTo({ url: '/pages/mybookings/mybookings' }), 1000)
  }).catch(() => {})
}

function weekCn(t) {
  if (!t) return ''
  const wk = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return wk[new Date(t.replace(/-/g, '/').replace('T', ' ')).getDay()]
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
.media-ph { height: 100%; background: linear-gradient(140deg, $pink-dk, #2a0a1d 60%, #160a12); display: flex; align-items: center; justify-content: center; }
.mp-dance { font-size: 56rpx; font-weight: 900; letter-spacing: 8rpx; color: rgba(255,255,255,.85); }
.mp-play { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); width: 110rpx; height: 110rpx; border-radius: 50%; background: rgba(255,45,155,.9); display: flex; align-items: center; justify-content: center; font-size: 40rpx; color: #fff; }
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
.i-tags { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 14rpx; }
.tag { background: $card2; color: $txt2; font-size: 22rpx; border-radius: 8rpx; padding: 6rpx 18rpx; }
.tag.pink-tag { background: rgba(255,45,155,.15); color: $pink2; }
.i-line { color: $txt2; font-size: 25rpx; margin-top: 14rpx; }
.checkin-tip { margin: 26rpx 30rpx 0; background: linear-gradient(90deg, rgba(255,45,155,.22), rgba(255,45,155,.06)); border: 1rpx solid rgba(255,45,155,.35); border-radius: 18rpx; padding: 22rpx 26rpx; }
.ct-line1 { font-size: 27rpx; font-weight: 700; }
.ct-line2 { color: $txt2; font-size: 23rpx; margin-top: 8rpx; }
.sec-card { margin: 22rpx 30rpx 0; padding: 24rpx 26rpx; }
.sc-h { display: flex; align-items: center; gap: 12rpx; }
.sc-t { font-size: 28rpx; font-weight: 800; border-left: 6rpx solid $pink; padding-left: 14rpx; }
.sc-b { display: block; color: $txt2; font-size: 25rpx; line-height: 1.8; margin-top: 14rpx; }
.sc-list { display: flex; flex-direction: column; gap: 8rpx; margin-top: 14rpx; }
.sc-li { color: $txt2; font-size: 25rpx; line-height: 1.7; }
.cta-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 30rpx calc(env(safe-area-inset-bottom) + 20rpx); background: linear-gradient(180deg, transparent, $bg 35%); }
.cta-btn { font-size: 32rpx; font-weight: 800; padding: 8rpx 0; }
.cta-btn.disabled { opacity: .5; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.65); z-index: 1000; display: flex; align-items: flex-end; }
.sheet { width: 100%; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 32rpx calc(env(safe-area-inset-bottom) + 32rpx); }
.s-title { font-size: 32rpx; font-weight: 800; text-align: center; }
.s-text { color: $txt2; font-size: 26rpx; line-height: 1.7; margin: 24rpx 0 32rpx; }
.s-btns { display: flex; gap: 20rpx; }
.s-cancel { flex: 1; background: $card2; color: $txt2; font-size: 28rpx; border-radius: 18rpx; }
.s-ok { flex: 1; font-size: 28rpx; }
</style>
