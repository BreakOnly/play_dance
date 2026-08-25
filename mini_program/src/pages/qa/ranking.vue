<template>
  <scroll-view scroll-y class="page">
    <!-- 规则说明 -->
    <view class="rule">按点赞数排序 · 每日更新</view>

    <!-- 榜单 -->
    <view v-for="(v, i) in list" :key="v.id" class="item card" :class="{ top1: i === 0 }" @tap="play(v)">
      <view class="no" :class="'no' + (i + 1)">{{ i + 1 }}</view>
      <view class="cover" :style="coverBg(v.coverUrl)">
        <image v-if="v.coverUrl && !isPreset(v.coverUrl)" :src="fileUrl(v.coverUrl)" mode="aspectFill" class="cimg" />
        <view v-else class="ph">▶</view>
        <view class="dur">{{ fmtDur(v.duration) }}</view>
      </view>
      <view class="grow">
        <view class="title">{{ v.title }}</view>
        <view class="desc">{{ v.description || '' }}</view>
      </view>
      <view class="likes">♥ {{ fmtLikes(v.likeCount) }}</view>
    </view>
    <view v-if="!list.length" class="empty">虚位以待</view>

    <!-- 底部提示（视频由工作室上传，用户仅观看点赞） -->
    <view class="cta-dock">
      <view class="cta-tip">视频由工作室发布 · 点赞支持你喜欢的作品</view>
    </view>
  </scroll-view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, fileUrl } from '@/api'

const list = ref([])

onShow(() => {
  api.videoRanking().then((l) => { list.value = l || [] }).catch(() => {})
})

function play(v) {
  api.videoPlayUrl(v.id).then((d) => {
    uni.navigateTo({ url: '/pages/webview/webview?url=' + encodeURIComponent(d.url) })
  }).catch(() => {})
}

/** 预设封面(preset:N)渐变 */
const presetBgs = ['linear-gradient(135deg, #FF2D9B, #7a0f4d)', 'linear-gradient(135deg, #8a5cf6, #2d1b69)', 'linear-gradient(135deg, #ff9f43, #b3126b)']
function isPreset(u) { return /^preset:\d/.test(u || '') }
function coverBg(u) {
  if (!isPreset(u)) return {}
  return { background: presetBgs[Number(u.split(':')[1])] || presetBgs[0] }
}
function fmtDur(sec) {
  if (sec == null) return ''
  const m = Math.floor(sec / 60), s = sec % 60
  return m + ':' + String(s).padStart(2, '0')
}
function fmtLikes(n) {
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return n
}
</script>

<style lang="scss" scoped>
.page { height: 100vh; box-sizing: border-box; background: $bg; padding-bottom: 220rpx; }
.rule { color: $txt2; font-size: 24rpx; text-align: center; padding: 24rpx 0 16rpx; }
.item { margin: 0 24rpx 18rpx; padding: 22rpx; display: flex; align-items: center; gap: 18rpx; }
.item.top1 { border: 1rpx solid $pink; box-shadow: 0 0 24rpx rgba(255,45,155,.25); }
.no { width: 52rpx; height: 52rpx; border-radius: 50%; background: $card2; color: $txt2; font-weight: 900; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.no.no1 { background: linear-gradient(135deg, $pink, $pink2); color: #fff; }
.no.no2 { background: linear-gradient(135deg, #8a5cf6, #b388ff); color: #fff; }
.no.no3 { background: linear-gradient(135deg, #ff9f43, #ffd166); color: #fff; }
.cover { position: relative; width: 140rpx; height: 104rpx; border-radius: 12rpx; overflow: hidden; background: $card2; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cimg { width: 100%; height: 100%; }
.ph { font-size: 48rpx; color: $pink2; }
.dur { position: absolute; right: 8rpx; bottom: 8rpx; background: rgba(0,0,0,.65); color: #fff; font-size: 20rpx; border-radius: 8rpx; padding: 2rpx 10rpx; }
.title { font-size: 28rpx; font-weight: 700; }
.desc { color: $txt2; font-size: 23rpx; margin-top: 8rpx; }
.likes { color: $pink2; font-size: 26rpx; font-weight: 800; flex-shrink: 0; }
.empty { color: $txt3; font-size: 26rpx; text-align: center; padding: 60rpx 0; }
.cta-dock { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 32rpx calc(env(safe-area-inset-bottom) + 20rpx); background: linear-gradient(180deg, transparent, $bg 40%); }
.cta { font-size: 30rpx; font-weight: 800; padding: 6rpx 0; }
.cta-tip { color: $txt3; font-size: 21rpx; text-align: center; margin-top: 12rpx; }
</style>
