<template>
  <view class="login">
    <view class="logo">M</view>
    <view class="title">{{ studio.name || '街舞工作室' }}</view>
    <view class="sub">咖啡就位 · 童心无穷 · 一起跳舞</view>
    <button class="loginBtn" :loading="busy" :disabled="busy" @tap="doLogin">微信一键登录</button>
    <view class="agree">登录前请阅读 <text @tap="openLegal(studio.termsUrl)">《用户协议》</text>和<text @tap="openLegal(studio.privacyUrl)">《隐私政策》</text></view>
  </view>
</template>

<script setup>
import { api } from '@/api'
import { setToken, setUser } from '@/utils/auth'
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { silentLogin } from '@/utils/request'
const studio = ref({}), busy = ref(false)
onLoad(() => api.studio().then(d => { studio.value = d }).catch(() => {}))
function openLegal(url) {
  if (!url) { uni.showToast({ title: '门店尚未配置该说明，请联系管理员', icon: 'none' }); return }
  uni.navigateTo({ url: '/pages/webview/webview?url=' + encodeURIComponent(url) })
}

async function doLogin() {
  if (busy.value) return
  busy.value = true
  try { await silentLogin(); uni.reLaunch({ url: '/pages/home/home' }) } finally { busy.value = false }
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
.agree { color: #b6b6c0; font-size: 26rpx; line-height: 1.8; margin-top: 30rpx; }.agree text { color:#ff6fb5; text-decoration:underline; }
</style>
