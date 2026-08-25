<script>
import { isLogin } from '@/utils/auth'
export default {
  onLaunch: function () {
    // 未登录则跳登录页
    if (!isLogin()) {
      uni.reLaunch({ url: '/pages/login/login' })
    }
  },
  onShow: function () {},
  onHide: function () {}
}
</script>

<style>
/* 全局公共样式（参考代码风格） */
page {
  --pink: #ff2d9b;
  --pink-2: #ff6fb5;
  --pink-deep: #b3126b;
  --panel: #1a1a1d;
  --panel-2: #222226;
  --muted: #9a9aa2;
  --line: #2e2e33;
  width: 100%;
  min-height: 100%;
  background: #0e0e0e;
  color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 28rpx;
}

page, .page-shell {
  backface-visibility: hidden;
}

view, text, image, button, navigator, input, scroll-view {
  box-sizing: border-box;
}

button::after { border: 0; }

.page-shell { min-height: 100vh; padding: 28rpx 28rpx 48rpx; }
.page-title { font-size: 42rpx; line-height: 1.25; font-weight: 800; }
.section-title { margin: 38rpx 0 20rpx; font-size: 32rpx; font-weight: 800; }
.muted { color: var(--muted); }
.pink { color: var(--pink); }
.card {
  background: #1a1a1d;
  border: 1rpx solid var(--line);
  border-radius: 28rpx;
}
.primary-button {
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  border-radius: 46rpx;
  background: linear-gradient(90deg, var(--pink), var(--pink-2));
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  box-shadow: 0 16rpx 38rpx rgba(237, 90, 154, 0.22);
}
.ghost-button {
  height: 82rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  border: 1rpx solid rgba(237, 90, 154, 0.55);
  border-radius: 41rpx;
  background: rgba(237, 90, 154, 0.08);
  color: #f484b4;
  font-size: 28rpx;
}
.safe-top { padding-top: calc(24rpx + env(safe-area-inset-top)); }
.safe-bottom { padding-bottom: calc(156rpx + env(safe-area-inset-bottom)); }
</style>
