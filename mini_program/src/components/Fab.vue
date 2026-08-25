<template>
  <!-- 中央凸起 FAB：手办兑换入口（悬浮于 tabBar 上方） -->
  <view class="fab" @tap="go">
    <view class="fab-icon">🎁</view>
  </view>
</template>

<script setup>
function go() {
  uni.navigateTo({ url: '/pages/redeem/redeem' })
}
</script>

<style lang="scss" scoped>
.fab {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(env(safe-area-inset-bottom) + 46rpx);
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, $pink, $pink2);
  box-shadow: 0 8rpx 30rpx rgba(255, 45, 155, .55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.fab-icon {
  font-size: 48rpx;
  line-height: 1;
}
</style>
