<template>
  <view class="home-page safe-top safe-bottom">
    <view class="home-top">
      <view>Hi, <text>{{ member.name || '舞者' }}</text> 👋</view>
      <view class="bell" @tap="showMessages">🔔<view class="bell-dot" v-if="unreadCount > 0"></view></view>
    </view>

    <view class="brand-hero">
      <image src="/static/logo.png" mode="widthFix"></image>
      <view class="brand-copy">
        <text>玩舞尚志</text>
        <text>DANCE CASUALLY · 让热爱自由发生</text>
      </view>
      <text class="hot-label">🔥 本周热门</text>
    </view>

    <!-- 新人体验课卡片（标题/副标题/价格/原价 均从 config 字典读取，后台可改） -->
    <view class="trial-card" @tap="openService">
      <view class="trial-copy-wrap">
        <text class="trial-title">{{ trial.title || '新人体验课 2 节' }}</text>
        <text class="trial-copy">{{ trial.subtitle || '爵士 / K-POP / Hiphop 任选' }}</text>
      </view>
      <view class="trial-right">
        <view class="trial-price-box">
          <text class="trial-price"><text class="trial-rmb">¥</text>{{ trial.price || '9.9' }}</text>
          <text class="trial-origin">原价 ¥{{ trial.originPrice || '199' }}</text>
        </view>
        <text class="trial-arrow">›</text>
      </view>
    </view>

    <view class="guide-card card">
      <view class="guide-head"><text>新手指南</text><text>3 步开启街舞之旅</text></view>
      <view class="steps">
        <view class="step"><view class="step-number">1</view><text>购买体验课</text></view>
        <view class="step"><view class="step-number">2</view><text>选择课程</text></view>
        <view class="step off"><view class="step-number">3</view><text>到店试课</text></view>
      </view>
    </view>

    <view class="section-title" v-if="videos.length"><view>视频推荐</view><text @tap="goSchedule">更多 ›</text></view>
    <scroll-view v-if="videos.length" class="video-scroll" scroll-x enhanced :show-scrollbar="false">
      <view class="video-row">
        <view v-for="v in videos" :key="v.id" class="video-card" @tap="play(v)">
          <view>▶</view>
          <text>{{ v.title }}</text>
          <text>{{ v.description || '' }}</text>
        </view>
      </view>
    </scroll-view>

    <view class="contact-strip" @tap="callStudio">
      <view><text>玩舞尚志</text><text>购课与新人试课请电话咨询</text></view>
      <text>{{ servicePhone }} ›</text>
    </view>

    <!-- 试课联系客服底部弹窗 -->
    <view class="sheet-mask" v-if="serviceSheet" @tap="serviceSheet = false">
      <view class="sheet-panel" @tap.stop>
        <view class="sheet-title">联系客服</view>
        <view class="sheet-body">请拨打电话购买体验课：{{ servicePhone || '400-888-6688' }}</view>
        <view class="sheet-btn" @tap="serviceSheet = false">我知道了</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, fileUrl } from '@/api'

const member = ref({})
const trial = ref({})
const videos = ref([])
const guide = ref({})
const unreadCount = ref(0)
const servicePhone = ref('')
const serviceSheet = ref(false)

onShow(() => {
  api.home().then((d) => {
    member.value = d.account || {}
    trial.value = d.trial || {}
    videos.value = d.videos || []
    guide.value = d.guide || {}
    unreadCount.value = d.unreadCount || 0
    servicePhone.value = d.servicePhone || ''
  }).catch(() => {})
})

function play(v) {
  api.videoPlayUrl(v.id).then((d) => {
    uni.navigateTo({ url: '/pages/webview/webview?url=' + encodeURIComponent(d.url) })
  }).catch(() => {})
}

function callStudio() {
  if (servicePhone.value) {
    uni.showModal({ title: '联系舞房', content: '请拨打电话：' + servicePhone.value, showCancel: false, confirmColor: '#ff2d9b' })
  }
}

function showMessages() {
  uni.showToast({ title: '暂无新的消息', icon: 'none' })
}

function openService() { serviceSheet.value = true }

function goSchedule() { uni.switchTab({ url: '/pages/booking/booking' }) }
</script>

<style lang="scss" scoped>
.home-page { min-height: 100vh; padding: calc(74rpx + env(safe-area-inset-top)) 28rpx 44rpx; background: #0d0d0e; color: #fff; }
.home-top { height: 78rpx; display: flex; align-items: center; justify-content: space-between; font-size: 38rpx; font-weight: 900; }
.home-top > view:first-child text { color: #ff62ad; }
.bell { position: relative; width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; font-size: 34rpx; }
.bell-dot { position: absolute; width: 13rpx; height: 13rpx; border-radius: 50%; top: 8rpx; right: 8rpx; background: #ff2d9b; }
.brand-hero { height: 310rpx; border: 1rpx solid #34343b; border-radius: 38rpx; background: radial-gradient(circle at 76% 18%, rgba(255,45,155,.22), transparent 44%), linear-gradient(135deg,#19191c,#111114); overflow: hidden; position: relative; display: flex; align-items: center; justify-content: center; }
.brand-hero image { width: 82%; opacity: .95; }
.brand-copy { position: absolute; left: 28rpx; right: 28rpx; bottom: 28rpx; display: flex; justify-content: space-between; align-items: end; }
.brand-copy text:first-child { font-size: 28rpx; font-weight: 900; }
.brand-copy text:last-child { font-size: 19rpx; color: #b9b5c1; }
.hot-label { position: absolute; left: 26rpx; top: 24rpx; padding: 10rpx 19rpx; border: 1rpx solid #424249; border-radius: 22rpx; background: rgba(8,8,10,.82); font-size: 20rpx; }
.trial-card { min-height: 168rpx; margin: 24rpx 0; padding: 25rpx 30rpx; border-radius: 32rpx; background: linear-gradient(120deg,#ff2495,#b10f64 74%,#641042); box-shadow: 0 18rpx 45rpx rgba(255,28,139,.24); display: flex; align-items: center; justify-content: space-between; }
.trial-copy-wrap { min-width: 0; display: flex; flex-direction: column; }
.trial-title { font-size: 34rpx; font-weight: 900; }
.trial-copy { margin-top: 8rpx; font-size: 21rpx; color: rgba(255,255,255,.82); }
.trial-right { flex: none; margin-left: 16rpx; display: flex; align-items: center; gap: 18rpx; }
.trial-price-box { display: flex; flex-direction: column; align-items: flex-end; }
.trial-price { line-height: 1; color: #fff; font-size: 46rpx; font-weight: 900; }
.trial-price .trial-rmb { margin-right: 4rpx; font-size: 26rpx; font-weight: 900; }
.trial-origin { margin-top: 10rpx; color: rgba(255,255,255,.66); font-size: 19rpx; text-decoration: line-through; }
.trial-arrow { width: 58rpx; height: 58rpx; border-radius: 50%; background: rgba(255,255,255,.12); display: flex; align-items: center; justify-content: center; font-size: 44rpx; }
.guide-card { padding: 28rpx 26rpx 24rpx; }
.guide-head { display: flex; justify-content: space-between; align-items: center; }
/* 标题前 Blackpink 粉色圆头竖条 */
.guide-head text:first-child { position: relative; padding-left: 18rpx; font-size: 28rpx; font-weight: 800; }
.guide-head text:first-child::before { content: ''; position: absolute; left: 0; top: 4rpx; width: 7rpx; height: 28rpx; border-radius: 5rpx; background: linear-gradient(180deg,#ff2d9b,#ff6fb5); }
.guide-head text:last-child { font-size: 20rpx; color: #a5a5ad; }
.steps { margin-top: 46rpx; display: flex; position: relative; }
.steps::before { content: ''; position: absolute; left: 15%; right: 15%; top: 19rpx; height: 4rpx; background: #ff2796; }
.step { flex: 1; text-align: center; position: relative; z-index: 1; }
/* 1、2 常亮：粉色渐变填满 + 白色数字（照设计稿固定） */
.step-number { width: 39rpx; height: 39rpx; margin: auto; border-radius: 50%; background: linear-gradient(135deg,#ff2d9b,#ff6fb5); color: #fff; font-size: 20rpx; font-weight: 800; display: flex; align-items: center; justify-content: center; }
/* 第 3 步：黑灰空心 —— 灰描边、透明填充、灰数字 */
.step-number.off { border: 2rpx solid #4a4a52; background: transparent; color: #777780; }
.step text { display: block; margin-top: 13rpx; font-size: 21rpx; color: #fff; }
.step.off text { color: #777780; }
.section-title { height: 84rpx; display: flex; align-items: center; justify-content: space-between; }
.section-title view { padding-left: 18rpx; position: relative; font-size: 29rpx; font-weight: 800; }
.section-title view::before { content: ''; position: absolute; left: 0; top: 4rpx; width: 7rpx; height: 28rpx; border-radius: 5rpx; background: #ff2694; }
.section-title text { color: #aaaabd; font-size: 21rpx; }
.video-scroll { width: 100%; white-space: nowrap; }
.video-row { display: inline-flex; gap: 18rpx; }
.video-card { width: 270rpx; height: 250rpx; flex: none; border: 1rpx solid #37373d; border-radius: 26rpx; background: linear-gradient(#27131f 0 63%,#19191c 63%); overflow: hidden; }
.video-card view { width: 62rpx; height: 62rpx; margin: 62rpx auto 46rpx; border-radius: 50%; background: #e92789; display: flex; align-items: center; justify-content: center; }
.video-card > text { display: block; padding: 0 20rpx; }
.video-card > text:nth-child(2) { font-size: 24rpx; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.video-card > text:last-child { margin-top: 7rpx; font-size: 20rpx; color: #aeb0c4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.contact-strip { margin-top: 25rpx; padding: 24rpx 26rpx; border: 1rpx solid #4b1732; border-radius: 25rpx; background: #24101b; display: flex; justify-content: space-between; align-items: center; }
.contact-strip view { display: flex; flex-direction: column; }
.contact-strip view text:first-child { font-size: 25rpx; font-weight: 800; }
.contact-strip view text:last-child { margin-top: 6rpx; font-size: 20rpx; color: #ff67b1; }
.contact-strip > text { font-size: 21rpx; color: #fff; }
.sheet-mask { position: fixed; inset: 0; z-index: 999; background: rgba(0,0,0,.55); display: flex; align-items: flex-end; }
.sheet-panel { width: 100%; padding: 56rpx 40rpx calc(44rpx + env(safe-area-inset-bottom)); border-radius: 32rpx 32rpx 0 0; background: #2a2a2e; animation: sheet-up .25s ease-out; }
.sheet-title { text-align: center; font-size: 32rpx; font-weight: 800; color: #fff; }
.sheet-body { margin-top: 30rpx; text-align: center; font-size: 26rpx; color: #c9c9d1; }
.sheet-btn { margin-top: 56rpx; height: 88rpx; border-radius: 44rpx; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); color: #fff; font-size: 28rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
@keyframes sheet-up { from { transform: translateY(100%); } to { transform: translateY(0); } }
</style>