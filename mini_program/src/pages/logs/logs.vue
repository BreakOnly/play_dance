<template>
  <view class="page">
    <view class="tabs"><button @tap="mode = 'hours'; load()">课时明细</button><button @tap="mode = 'points'; load()">积分明细</button></view>
    <scroll-view scroll-y class="list">
      <view v-if="rows.length===0" class="tip">暂无记录</view>
      <view v-for="r in rows" :key="r.id" class="it">
        <view class="left">
          <view class="ty">{{ typeText(r.type) }}</view>
          <view class="rm">{{ r.remark || '' }}</view>
          <view class="tm">{{ fmt(r.createdAt) }}</view>
        </view>
        <view class="amt" :class="{ plus: r.amount>0, minus: r.amount<0 }">
          {{ r.amount>0 ? '+'+r.amount : r.amount }}
          <view class="bal">余 {{ r.balanceAfter }}</view>
        </view>
      </view>
      <view style="height: 30rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'

const rows = ref([])
const mode = ref('hours')

onLoad(() => load())
function load() {
  (mode.value === 'hours' ? api.hourLogs() : api.pointLogs()).then((d) => { rows.value = d || [] }).catch(() => {})
}
function typeText(t) {
  return {
    recharge: '充值', gift: '赠送', lock: '约课锁定', deduct: '签到核销', refund: '取消退回',
    noshow: '爽约核销', expire: '过期', manual: '管理员调整', checkin: '签到积分', redeem: '积分兑换'
  }[t] || t
}
function fmt(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' }
</script>

<style lang="scss" scoped>
.page { height: 100vh; display: flex; flex-direction: column; background: $bg; }
.tabs { display:flex; padding:20rpx; gap:20rpx; }.tabs button { flex:1; color:#fff; background:#331b2b; font-size:26rpx; }
.list { flex: 1; padding: 0 24rpx; box-sizing: border-box; }
.it { display: flex; justify-content: space-between; padding: 24rpx 0; border-bottom: 1rpx solid $line; }
.ty { font-size: 28rpx; font-weight: 600; }
.rm { color: $txt2; font-size: 22rpx; margin-top: 6rpx; }
.tm { color: $txt3; font-size: 22rpx; margin-top: 6rpx; }
.amt { text-align: right; font-size: 34rpx; font-weight: 800; color: $txt2; }
.amt.plus { color: #4cd964; }
.amt.minus { color: $pink2; }
.bal { color: $txt3; font-size: 22rpx; font-weight: 400; margin-top: 6rpx; }
.tip { color: $txt3; text-align: center; padding: 60rpx; }
</style>
