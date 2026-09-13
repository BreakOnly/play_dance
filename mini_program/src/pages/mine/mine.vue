<template>
  <view class="member-page safe-top safe-bottom">
    <view class="profile-head">
      <image src="/static/logo.png" mode="widthFix"></image>
      <view class="edit-button" @tap="editProfile">✎ 编辑资料</view>
      <view class="profile-row">
        <view class="avatar">舞</view>
        <view>
          <view class="member-name">{{ member.name || '舞者' }}</view>
          <text>▣ {{ member.phone || '未绑定' }} · 正式会员</text>
        </view>
      </view>
    </view>

    <view class="balance-card card">
      <view class="balance-top"><text>课时余额</text><button @tap="callStudio">充值</button></view>
      <view class="balance-number"><text>{{ member.availableHours || 0 }}</text><text>课时</text></view>
      <view class="balance-meta">
        <view><text>已冻结</text><text>{{ member.frozenHours || 0 }} 课时</text></view>
        <view><text>有效期至</text><text>{{ member.validUntil || '长期有效' }}</text></view>
      </view>
      <text class="balance-tip">预约后冻结，上课完成后正式扣课</text>
    </view>

    <view v-if="member.expiringHours" class="expire-card">
      <view><text>会员课时临期提醒</text><text>你有 {{ member.expiringHours }} 课时临近有效期</text></view>
      <text @tap="goSchedule">去约课 ›</text>
    </view>

    <view class="section-title">我的服务</view>
    <view class="services card">
      <view @tap="goBookings"><view class="service-icon">约</view><text>我的约课</text><text>{{ member.upcomingCount || 0 }} 节待上课</text><text>›</text></view>
      <view @tap="showMessages"><view class="service-icon">信</view><text>消息通知</text><text>{{ unread ? unread + ' 条未读' : '查看消息' }}</text><text>›</text></view>
      <view @tap="goLogs"><view class="service-icon">账</view><text>课时与积分明细</text><text></text><text>›</text></view>
      <view @tap="goRedeem"><view class="service-icon">兑</view><text>积分兑换</text><text>{{ member.pointsBalance ?? 0 }} 积分</text><text>›</text></view>
      <view @tap="showRule"><view class="service-icon">规</view><text>预约与取消规则</text><text></text><text>›</text></view>
      <view @tap="callStudio"><view class="service-icon">电</view><text>联系舞房</text><text>{{ servicePhone }}</text><text>›</text></view>
    </view>
    <view class="studio-foot">玩舞尚志 · DANCE CASUALLY · v1.0</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/api'

const member = ref({})
const servicePhone = ref('')
const unread = ref(0), rules = ref({})

onShow(() => {
  api.unreadCount().then(d => { unread.value = typeof d === 'number' ? d : d.count || 0 }).catch(() => {})
  api.getConfig().then(d => { rules.value = d }).catch(() => {})
  api.member().then((d) => {
    member.value = d || {}
  }).catch(() => {})
  api.home().then((d) => {
    servicePhone.value = d.servicePhone || ''
  }).catch(() => {})
})

function editProfile() {
  uni.navigateTo({ url: '/pages/profile/profile' }); return
  uni.showModal({
    title: '修改名字',
    editable: true,
    placeholderText: '请输入你的名字',
    content: member.value.name || '',
    confirmColor: '#ff2d9b',
    success: async (result) => {
      if (!result.confirm) return
      const name = (result.content || '').trim()
      if (!name) { uni.showToast({ title: '名字不能为空', icon: 'none' }); return }
      api.updateMemberName(name).then(() => {
        member.value.name = name
        uni.showToast({ title: '已更新', icon: 'success' })
      }).catch(() => {})
    }
  })
}

function callStudio() {
  const p = servicePhone.value
  if (!p) { uni.showToast({ title: '门店暂未设置联系电话', icon: 'none' }); return }
  uni.showModal({ title: '联系舞房', content: '请拨打电话：' + p, showCancel: false, confirmColor: '#ff2d9b' })
}

function showMessages() { uni.navigateTo({ url: '/pages/notifications/notifications' }) }
function goLogs() { uni.navigateTo({ url: '/pages/logs/logs' }) }
function goRedeem() { uni.switchTab({ url: '/pages/redeem/redeem' }) }
function showRule() {
  uni.showModal({
    title: '预约与取消规则',
    content: `预约按课程标注冻结课时，签到后核销；候补不冻结，转正才冻结。距开课不足 ${rules.value.cancelFreeHours ?? 6} 小时仅管理员可以取消。取消时原批次已到期的课时不会恢复为可用。请使用微信扫一扫门店签到码。`,
    showCancel: false, confirmColor: '#ff2d9b'
  })
}

function goBookings() { uni.navigateTo({ url: '/pages/mybookings/mybookings' }) }
function goSchedule() { uni.switchTab({ url: '/pages/booking/booking' }) }
</script>

<style lang="scss" scoped>
.member-page { min-height: 100vh; padding: 0 28rpx 45rpx; background: #0d0d0e; color: #fff; }
.profile-head { height: 230rpx; margin: 0 -28rpx; padding: 22rpx 35rpx; background: linear-gradient(115deg,#ba0f65,#4b0a34); position: relative; overflow: hidden; }
.profile-head > image { position: absolute; width: 280rpx; right: 24rpx; top: 14rpx; opacity: .27; }
.edit-button { position: absolute; right: 30rpx; top: 22rpx; padding: 10rpx 18rpx; border-radius: 25rpx; background: rgba(0,0,0,.25); font-size: 20rpx; }
.profile-row { position: absolute; left: 35rpx; bottom: 32rpx; display: flex; align-items: center; gap: 20rpx; }
.avatar { width: 92rpx; height: 92rpx; border: 3rpx solid #fff; border-radius: 50%; background: #281323; display: flex; align-items: center; justify-content: center; font-size: 38rpx; font-weight: 900; }
.profile-row > view:last-child { display: flex; flex-direction: column; }
.member-name { font-size: 34rpx; font-weight: 900; }
.profile-row > view:last-child > text:last-child { margin-top: 9rpx; font-size: 20rpx; color: rgba(255,255,255,.78); }
.balance-card { min-height: 258rpx; margin-top: -18rpx; padding: 26rpx; position: relative; z-index: 2; }
.balance-top { display: flex; justify-content: space-between; color: #adb0c0; font-size: 22rpx; }
.balance-top button { margin: 0; padding: 8rpx 28rpx; border: 0; border-radius: 28rpx; background: #ff4ca3; color: #fff; font-size: 21rpx; line-height: 1.6; }
.balance-number { display: flex; align-items: end; gap: 10rpx; }
.balance-number text:first-child { font-size: 70rpx; font-weight: 900; }
.balance-number text:last-child { padding-bottom: 13rpx; font-size: 22rpx; }
.balance-meta { padding-top: 19rpx; border-top: 1rpx solid #35353b; display: flex; }
.balance-meta view { flex: 1; display: flex; flex-direction: column; }
.balance-meta view text:first-child { color: #888a98; font-size: 19rpx; }
.balance-meta view text:last-child { margin-top: 5rpx; font-size: 22rpx; font-weight: 700; }
.balance-tip { display: block; margin-top: 18rpx; color: #777986; font-size: 19rpx; }
.expire-card { margin-top: 20rpx; padding: 24rpx; border-radius: 28rpx; background: linear-gradient(110deg,#ff168b,#7e0b4f); display: flex; align-items: center; justify-content: space-between; }
.expire-card view { display: flex; flex-direction: column; }
.expire-card view text:first-child { font-size: 26rpx; font-weight: 800; }
.expire-card view text:last-child { margin-top: 7rpx; font-size: 20rpx; }
.expire-card > text { padding: 10rpx 18rpx; border-radius: 23rpx; background: #fff; color: #ff2592; font-size: 20rpx; }
.section-title { padding: 31rpx 0 17rpx; font-size: 29rpx; font-weight: 900; }
.services { overflow: hidden; }
.services > view { min-height: 94rpx; padding: 0 21rpx; border-bottom: 1rpx solid #35353b; display: grid; grid-template-columns: 56rpx 1fr auto 24rpx; align-items: center; gap: 9rpx; }
.services > view:last-child { border-bottom: 0; }
.service-icon { width: 45rpx; height: 45rpx; border-radius: 15rpx; background: #3d162d; color: #ff5eab; font-size: 20rpx; display: flex; align-items: center; justify-content: center; }
.services > view > text:nth-child(2) { font-size: 24rpx; font-weight: 700; }
.services > view > text:nth-child(3) { color: #888a98; font-size: 19rpx; }
.services > view > text:last-child { color: #8d8e9d; }
.studio-foot { padding: 33rpx 0 6rpx; color: #656674; text-align: center; font-size: 19rpx; }
</style>
