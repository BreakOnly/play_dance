<template>
  <view class="page-shell">
    <view class="card result">
      <view class="title">{{ busy ? '正在确认签到…' : result ? '签到成功' : '签到未完成' }}</view>
      <template v-if="result">
        <view>{{ result.alreadyChecked ? '本课次已签到，不会重复扣课时。' : '已完成本课次签到。' }}</view>
        <view>本次课时 {{ result.costHours }} · 签到积分 {{ result.pointsGain }}</view>
        <view>剩余可用 {{ result.balanceAfter }} 课时</view>
      </template>
      <view v-else-if="!busy">{{ message }}</view>
      <button v-if="!busy && !result && scene" class="primary-button" @tap="checkin">重试签到</button>
      <button v-if="!busy" class="ghost-button" @tap="goBookings">查看我的约课</button>
    </view>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '@/api'
import { silentLogin } from '@/utils/request'
import { isLogin } from '@/utils/auth'
const scene = ref(''), busy = ref(false), result = ref(null), message = ref('请使用微信扫一扫门店展示的签到码。')
function accept(options) {
  let next = ''
  try { next = decodeURIComponent(options?.scene || '') } catch {}
  if (!/^[0-9a-f]{32}$/.test(next)) return
  if (next !== scene.value) { scene.value = next; result.value = null; checkin() }
}
onLoad(accept)
onShow(() => {
  // #ifdef MP-WEIXIN
  const entry = uni.getEnterOptionsSync?.()
  if (entry?.path === 'pages/checkin/result') accept(entry.query)
  // #endif
})
async function checkin() {
  if (busy.value || !scene.value) return
  busy.value = true
  const current = scene.value
  try {
    if (!isLogin()) await silentLogin()
    const data = await api.scanCheckin(current)
    if (scene.value === current) result.value = data
  } catch (e) { message.value = e?.message || '网络连接失败，请重试或联系工作人员。' }
  finally { busy.value = false; if (current !== scene.value) checkin() }
}
function goBookings() { uni.redirectTo({ url: '/pages/mybookings/mybookings' }) }
</script>
<style scoped>
.result { padding: 48rpx 32rpx; margin-top: 80rpx; text-align: center; line-height: 2; }
.title { font-size: 40rpx; font-weight: 800; margin-bottom: 24rpx; }
button { margin-top: 32rpx; }
</style>
