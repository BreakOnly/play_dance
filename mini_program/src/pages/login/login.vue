<template>
  <view class="login">
    <view class="logo">M</view>
    <view class="title">XX 街舞工作室</view>
    <view class="sub">咖啡就位 · 童心无穷 · 一起跳舞</view>
    <button class="loginBtn" @tap="doLogin">微信一键登录</button>
    <view class="agree">登录即代表同意《用户协议》《隐私政策》</view>
  </view>
</template>

<script setup>
import { api } from '@/api'
import { setToken, setUser } from '@/utils/auth'

function doLogin() {
  // #ifdef MP-WEIXIN
  uni.login({
    provider: 'weixin',
    success: (res) => exchange(res.code),
    fail: () => uni.showToast({ title: '微信登录失败', icon: 'none' })
  })
  // #endif
  // #ifndef MP-WEIXIN
  exchange('devcode_001')
  // #endif
}

function exchange(code) {
  api.login(code).then((data) => {
    setToken(data.token)
    setUser({ id: data.userId, name: data.name })
    uni.reLaunch({ url: '/pages/home/home' })
  })
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 30%, #2a0f1f 0%, #0E0E0E 60%);
  padding: 0 60rpx;
}
.logo {
  width: 130rpx; height: 130rpx; border-radius: 50%;
  background: linear-gradient(135deg, $pink, $pink2);
  color: #fff; font-size: 64rpx; font-weight: 900;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 10rpx 40rpx rgba(255,45,155,.5);
}
.title { color: #fff; font-size: 40rpx; font-weight: 800; margin-top: 36rpx; }
.sub { color: $txt2; font-size: 24rpx; margin-top: 14rpx; }
.loginBtn {
  margin-top: 100rpx; width: 100%;
  background: linear-gradient(90deg, $pink, $pink2);
  color: #fff; font-size: 32rpx; font-weight: 700;
  border-radius: 44rpx; padding: 22rpx 0;
}
.agree { color: $txt3; font-size: 22rpx; margin-top: 30rpx; }
</style>
