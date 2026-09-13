<template>
  <scroll-view scroll-y class="page">
    <!-- 待上课分组 -->
    <view class="grp-title">待上课 · {{ upcomings.length }}</view>
    <view v-if="!upcomings.length" class="tip">暂无待上课</view>
    <view v-for="b in upcomings" :key="b.id" class="bcard card" @tap="goDetail(b)">
      <view class="date-block">
        <view class="d-day">{{ dayNum(b.startTime) }}</view>
        <view class="d-week">{{ weekCn(b.startTime) }}</view>
      </view>
      <view class="grow">
        <view class="row between">
          <view class="cn">{{ b.courseName }}</view>
          <view class="st" :class="b.status">{{ statusText(b.status) }}</view>
        </view>
        <view class="meta">{{ b.teacherName || '老师' }} · {{ b.room || '' }} · {{ storeName(b) }}</view>
        <view class="meta">{{ fmtTime(b.startTime) }} - {{ fmtTime(b.endTime) }}</view>
        <view v-if="b.status==='waitlist'" class="wl">候补第 {{ b.waitlistNo }} 位</view>
        <view class="foot-row">
          <button v-if="(b.status==='booked' || b.status==='waitlist') && canCancel(b)" class="cancel" @tap.stop="openCancel(b)">取消预约</button>
          <view v-else-if="b.status==='booked'" class="late-tip">距开课不足 {{ cancelFreeHours }} 小时，如需取消请联系门店</view>
        </view>
      </view>
    </view>

    <!-- 已使用分组 -->
    <view class="grp-title used">已使用 · {{ useds.length }}</view>
    <view v-if="!useds.length" class="tip">暂无记录</view>
    <view v-for="b in useds" :key="b.id" class="bcard card used-card" @tap="goDetail(b)">
      <view class="date-block">
        <view class="d-day">{{ dayNum(b.startTime) }}</view>
        <view class="d-week">{{ weekCn(b.startTime) }}</view>
      </view>
      <view class="grow">
        <view class="row between">
          <view class="cn">{{ b.courseName }}</view>
          <view class="st" :class="b.status">{{ statusText(b.status) }}</view>
        </view>
        <view class="meta">{{ b.teacherName || '老师' }} · {{ b.room || '' }} · {{ storeName(b) }}</view>
        <view class="meta">{{ fmtTime(b.startTime) }} - {{ fmtTime(b.endTime) }}</view>
        <view v-if="b.status==='checked'" class="points">+{{ b.checkinPoints ?? 0 }} 积分</view>
      </view>
    </view>

    <!-- 积分提示条 -->
    <view class="p-tip">到店签到每次 +{{ pointsPerCheckin }} 积分，可在手办商城兑换</view>

    <!-- 取消确认弹层 -->
    <view v-if="cancelTarget" class="mask" @tap="cancelTarget = null">
      <view class="sheet card" @tap.stop>
        <view class="s-title">取消预约</view>
        <view class="s-text">{{ cancelFreeText }}</view>
        <view class="s-btns">
          <button class="s-cancel" @tap="cancelTarget = null">再想想</button>
          <button class="btn-primary s-ok" @tap="doCancel">确认取消</button>
        </view>
      </view>
    </view>
  </scroll-view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/api'

const all = ref([])
const cancelTarget = ref(null)
const cancelFreeHours = ref(6)
const pointsPerCheckin = ref(100)

/** 待上课：booked + waitlist；已使用：checked/noshow/cancelled（原型只展示已签到与历史） */
const upcomings = computed(() => all.value.filter((b) => b.status === 'booked' || b.status === 'waitlist'))
const useds = computed(() => all.value.filter((b) => ['checked', 'noshow', 'cancelled'].includes(b.status)))

const cancelFreeText = computed(() => {
  const b = cancelTarget.value
  if (b?.status === 'waitlist') return `确定取消《${b.courseName || ''}》的候补吗？取消后当前候补位置将立即释放。`
  return `确定要取消预约《${cancelTarget.value?.courseName || ''}》吗？\n取消后冻结的课时将立即退回。`
})

/** 候补随时可取消；已约课仅开课 cancelFreeHours 小时之前可线上取消 */
function canCancel(b) {
  if (b.status === 'waitlist') return true
  if (!b.startTime) return false
  return (new Date(b.startTime.replace(/-/g, '/').replace('T', ' ')) - new Date()) / 3600000 >= cancelFreeHours.value
}

onShow(() => {
  api.myBookings().then((d) => { all.value = d || [] }).catch(() => {})
  api.getConfig().then((cfg) => {
    cancelFreeHours.value = cfg.cancelFreeHours ?? 6
    pointsPerCheckin.value = cfg.pointsPerCheckin ?? 100
  }).catch(() => {})
})

function openCancel(b) { cancelTarget.value = b }

function doCancel() {
  const b = cancelTarget.value
  api.cancelBooking(b.id).then(() => {
    cancelTarget.value = null
    uni.showToast({ title: `已取消：${b.courseName}，冻结课时已退回`, icon: 'none' })
    api.myBookings().then((d) => { all.value = d || [] })
  }).catch(() => {})
}

function goDetail(b) {
  if (b.scheduleId) uni.navigateTo({ url: '/pages/booking/detail?id=' + b.scheduleId })
}

function dayNum(t) { return t ? t.substring(8, 10) : '' }
function weekCn(t) {
  if (!t) return ''
  const wk = ['日', '一', '二', '三', '四', '五', '六']
  const month = Number(t.substring(5, 7))
  return month + '月·周' + wk[new Date(t.replace(/-/g, '/').replace('T', ' ')).getDay()]
}
function fmtTime(t) { return t ? t.substring(11, 16) : '' }
function storeName(b) { return b.storeName || '' }
function statusText(s) {
  return { booked: '待上课', checked: '已签到', cancelled: '已取消', noshow: '爽约', waitlist: '候补中' }[s] || s
}
</script>

<style lang="scss" scoped>
.page { height: 100vh; box-sizing: border-box; background: $bg; padding: 20rpx 24rpx 60rpx; }
.grp-title { font-size: 30rpx; font-weight: 800; padding: 16rpx 4rpx; }
.grp-title.used { margin-top: 20rpx; color: $txt2; }
.tip { color: $txt3; font-size: 25rpx; text-align: center; padding: 26rpx 0; }
.bcard { display: flex; gap: 20rpx; padding: 24rpx; margin-bottom: 18rpx; }
.used-card { opacity: .65; }
.date-block { width: 100rpx; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; background: $card2; border-radius: 14rpx; padding: 14rpx 0; }
.d-day { font-size: 40rpx; font-weight: 900; }
.d-week { color: $txt2; font-size: 22rpx; margin-top: 4rpx; }
.grow { flex: 1; min-width: 0; }
.row.between { display: flex; justify-content: space-between; align-items: center; }
.cn { font-size: 29rpx; font-weight: 700; }
.st { font-size: 23rpx; color: $txt2; flex-shrink: 0; }
.st.booked { color: $pink2; }
.st.checked { color: #7ee787; }
.st.noshow { color: #dd524d; }
.st.waitlist { color: #ffd166; }
.meta { color: $txt2; font-size: 23rpx; margin-top: 8rpx; }
.wl { color: #ffd166; font-size: 23rpx; margin-top: 6rpx; }
.points { color: #7ee787; font-size: 25rpx; font-weight: 800; margin-top: 10rpx; }
.foot-row { display: flex; justify-content: flex-end; align-items: center; margin-top: 12rpx; }
/* 临近开课（<6h）不可线上取消时的提示 */
.late-tip { color: $txt2; font-size: 22rpx; }
.cancel { background: transparent; border: 1rpx solid $pink; color: $pink2; font-size: 24rpx; border-radius: 999rpx; padding: 4rpx 30rpx; line-height: 2; }
.p-tip { margin-top: 30rpx; background: $pink-soft; border: 1rpx solid #4d1530; color: $pink2; font-size: 23rpx; border-radius: 14rpx; padding: 16rpx 20rpx; text-align: center; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.65); z-index: 1000; display: flex; align-items: flex-end; }
.sheet { width: 100%; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 32rpx calc(env(safe-area-inset-bottom) + 32rpx); }
.s-title { font-size: 32rpx; font-weight: 800; text-align: center; }
.s-text { color: $txt2; font-size: 26rpx; line-height: 1.7; margin: 24rpx 0 32rpx; }
.s-btns { display: flex; gap: 20rpx; }
.s-cancel { flex: 1; background: $card2; color: $txt2; font-size: 28rpx; border-radius: 18rpx; }
.s-ok { flex: 1; font-size: 28rpx; }
</style>
