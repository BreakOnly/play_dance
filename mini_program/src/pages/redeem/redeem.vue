<template>
  <scroll-view scroll-y class="page">
    <!-- 我的积分 -->
    <view class="points-bar card">
      <text class="coin">🪙</text>
      <view class="grow">
        <view class="lab">我的积分</view>
        <view class="val">{{ points }}</view>
      </view>
      <button class="mini-btn" @tap="showOrders = true">我的兑换</button>
    </view>

    <!-- 商品宫格 -->
    <view class="grid">
      <view v-for="g in goodsList" :key="g.id" class="gcard card">
        <view class="gimg">
          <image v-if="cover(g)" :src="cover(g)" mode="aspectFill" class="gimg-in" />
          <text v-else class="gph">🎁</text>
        </view>
        <view class="gname">{{ g.name }}</view>
        <view class="gpoints">{{ g.pointsRequired }}积分</view>
        <button class="btn-primary ex-btn" @tap="confirmRedeem(g)">兑换</button>
      </view>
      <view v-if="!goodsList.length" class="empty">暂无可兑换商品</view>
    </view>

    <view class="tip">商品由后台上架上价，兑换后积分自动扣除</view>

    <!-- 兑换确认弹窗 -->
    <view v-if="popupGoods" class="mask" @tap="popupGoods = null">
      <view class="dialog card" @tap.stop>
        <view class="d-ic">⚠️</view>
        <view class="d-t">确认兑换</view>
        <view class="d-c">是否扣除 <text class="pink">{{ popupGoods.pointsRequired }}</text> 积分兑换【<text class="pink">{{ popupGoods.name }}</text>】？</view>
        <view class="d-btns">
          <button class="d-cancel" @tap="popupGoods = null">取消</button>
          <button class="btn-primary d-ok" @tap="doRedeem">确认兑换</button>
        </view>
      </view>
    </view>

    <!-- 我的兑换记录弹层 -->
    <view v-if="showOrders" class="mask" @tap="showOrders = false">
      <view class="sheet card" @tap.stop>
        <view class="s-title">我的兑换</view>
        <scroll-view scroll-y class="s-list">
          <view v-for="o in orders" :key="o.id" class="oitem">
            <view class="row between">
              <text class="oname">{{ o.goodsName }}</text>
              <text class="ost" :class="o.status">{{ statusText(o.status) }}</text>
            </view>
            <view class="ometa">消耗 {{ o.pointsCost }} 积分 · {{ fmtDate(o.createdAt) }}</view>
          </view>
          <view v-if="!orders.length" class="empty">暂无兑换记录</view>
        </scroll-view>
        <button class="s-close" @tap="showOrders = false">关闭</button>
      </view>
    </view>

    <view style="height: 60rpx;"></view>
  </scroll-view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, fileUrl } from '@/api'

const points = ref(0)
const goodsList = ref([])
const popupGoods = ref(null)
const showOrders = ref(false)
const orders = ref([])

onShow(() => load())

function load() {
  api.home().then((d) => { points.value = d.account?.pointsBalance ?? 0 }).catch(() => {})
  api.goods('redeem').then((list) => { goodsList.value = list || [] }).catch(() => {})
  api.redeemOrders().then((list) => { orders.value = list || [] }).catch(() => {})
}

function cover(g) {
  const imgs = g.images
  if (Array.isArray(imgs) && imgs.length) return fileUrl(imgs[0])
  if (typeof imgs === 'string' && imgs.startsWith('[')) {
    try { const arr = JSON.parse(imgs); return arr.length ? fileUrl(arr[0]) : '' } catch (e) { return '' }
  }
  return ''
}

function confirmRedeem(g) { popupGoods.value = g }

function doRedeem() {
  const g = popupGoods.value
  api.redeem(g.id).then(() => {
    popupGoods.value = null
    uni.showToast({ title: '兑换成功！积分已扣除', icon: 'none' })
    load()
  }).catch(() => {})
}

function statusText(s) {
  return { pending: '待发放', to_ship: '待发货', shipping: '配送中', completed: '已完成', rejected: '已驳回' }[s] || s
}
function fmtDate(t) { return t ? String(t).substring(0, 10) : '' }
</script>

<style lang="scss" scoped>
.page { height: 100vh; box-sizing: border-box; background: $bg; }
.points-bar { margin: 24rpx; padding: 28rpx; display: flex; align-items: center; }
.coin { font-size: 48rpx; margin-right: 20rpx; }
.grow { flex: 1; }
.lab { color: $txt2; font-size: 24rpx; }
.val { font-size: 48rpx; font-weight: 900; margin-top: 4rpx; }
.mini-btn { background: transparent; border: 1rpx solid $pink; color: $pink2; font-size: 24rpx; border-radius: 999rpx; padding: 8rpx 28rpx; line-height: 1.6; }
.grid { display: flex; flex-wrap: wrap; padding: 0 12rpx; }
.gcard { width: calc(50% - 24rpx); margin: 12rpx; padding: 20rpx; box-sizing: border-box; text-align: center; }
.gimg { height: 220rpx; border-radius: 16rpx; background: $card2; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.gimg-in { width: 100%; height: 100%; }
.gph { font-size: 80rpx; }
.gname { font-size: 27rpx; font-weight: 700; margin-top: 16rpx; text-align: left; }
.gpoints { color: $pink2; font-size: 26rpx; font-weight: 700; margin: 10rpx 0 16rpx; text-align: left; }
.ex-btn { font-size: 26rpx; padding: 4rpx 0; }
.tip { color: $txt3; font-size: 22rpx; text-align: center; padding: 20rpx 0 40rpx; }
.empty { color: $txt3; font-size: 26rpx; text-align: center; padding: 40rpx 0; width: 100%; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.65); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.dialog { width: 560rpx; padding: 40rpx 36rpx; text-align: center; border-radius: 28rpx; }
.d-ic { font-size: 64rpx; }
.d-t { font-size: 34rpx; font-weight: 900; margin: 16rpx 0; }
.d-c { color: $txt2; font-size: 26rpx; line-height: 1.7; margin-bottom: 32rpx; }
.d-btns { display: flex; gap: 20rpx; }
.d-cancel { flex: 1; background: $card2; color: $txt2; font-size: 28rpx; border-radius: 18rpx; }
.d-ok { flex: 1; font-size: 28rpx; }
.sheet { position: fixed; left: 0; right: 0; bottom: 0; border-radius: 32rpx 32rpx 0 0; padding: 32rpx 32rpx calc(env(safe-area-inset-bottom) + 24rpx); max-height: 70vh; display: flex; flex-direction: column; }
.s-title { font-size: 32rpx; font-weight: 800; text-align: center; margin-bottom: 20rpx; }
.s-list { max-height: 50vh; }
.oitem { padding: 20rpx 0; border-bottom: 1rpx solid $line; }
.oname { font-size: 28rpx; font-weight: 700; }
.ost { font-size: 24rpx; color: $pink2; }
.ost.completed { color: #7ee787; }
.ost.rejected { color: $txt3; }
.ometa { color: $txt2; font-size: 23rpx; margin-top: 8rpx; }
.s-close { margin-top: 24rpx; background: $card2; color: $txt; font-size: 28rpx; border-radius: 18rpx; }
</style>
