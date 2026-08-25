<template>
  <view class="page">
    <view class="topbar" v-if="list.length">
      <text class="readall" @tap="readAll">全部已读</text>
    </view>
    <scroll-view scroll-y class="list">
      <view v-if="list.length===0" class="tip">暂无消息</view>
      <view v-for="n in list" :key="n.id" class="it card" :class="{ unread: n.isRead===0 }" @tap="read(n)">
        <view class="row between">
          <view class="tt">{{ n.title }}</view>
          <view class="tm">{{ fmt(n.createdAt) }}</view>
        </view>
        <view class="ct">{{ n.content }}</view>
      </view>
      <view style="height: 30rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/api'

const list = ref([])
onShow(() => load())
function load() { api.notifications().then((d) => { list.value = d || [] }).catch(() => {}) }
function read(n) { if (n.isRead === 0) api.readNotice(n.id).then(() => load()) }
function readAll() { api.readNotice().then(() => load()) }
function fmt(t) { return t ? t.substring(5, 16).replace('T', ' ') : '' }
</script>

<style lang="scss" scoped>
.page { height: 100vh; display: flex; flex-direction: column; }
.topbar { text-align: right; padding: 16rpx 24rpx; }
.readall { color: $pink2; font-size: 24rpx; }
.list { flex: 1; padding: 0 24rpx; box-sizing: border-box; }
.tip { color: $txt3; text-align: center; padding: 80rpx; }
.it { padding: 24rpx; margin-bottom: 16rpx; position: relative; }
.it.unread::before { content: ''; position: absolute; left: 10rpx; top: 28rpx; width: 12rpx; height: 12rpx; border-radius: 50%; background: $pink; }
.tt { font-size: 28rpx; font-weight: 700; padding-left: 16rpx; }
.tm { color: $txt3; font-size: 22rpx; }
.ct { color: $txt2; font-size: 24rpx; margin-top: 10rpx; padding-left: 16rpx; }
</style>
