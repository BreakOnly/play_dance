<template>
  <view class="page">
    <view class="card form">
      <view class="f">
        <text class="lb">姓名</text>
        <input class="ipt" v-model="form.name" placeholder="请输入姓名" placeholder-class="ph" />
      </view>
      <view class="f">
        <text class="lb">手机号</text>
        <input class="ipt" v-model="form.phone" type="number" placeholder="请输入手机号" placeholder-class="ph" />
      </view>
      <view class="f">
        <text class="lb">性别</text>
        <view class="genders">
          <text class="g" :class="{on: form.gender===1}" @tap="form.gender=1">男</text>
          <text class="g" :class="{on: form.gender===2}" @tap="form.gender=2">女</text>
        </view>
      </view>
      <view class="f">
        <text class="lb">生日</text>
        <picker mode="date" :value="form.birthday" @change="onDate">
          <view class="ipt val">{{ form.birthday || '请选择' }}</view>
        </picker>
      </view>
    </view>
    <button class="save" @tap="save">保存</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { setUser, getUser } from '@/utils/auth'

const form = ref({ name: '', phone: '', gender: 0, birthday: '' })

onLoad(() => {
  api.profile().then((p) => {
    form.value = {
      name: p.name || '', phone: p.phone || '', gender: p.gender || 0,
      birthday: p.birthday || ''
    }
  })
})
function onDate(e) { form.value.birthday = e.detail.value }
function save() {
  api.updateProfile(form.value).then((p) => {
    const u = getUser(); u.name = p.name; setUser(u)
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  })
}
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.form { padding: 10rpx 24rpx; }
.f { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid $line; }
.f:last-child { border-bottom: none; }
.lb { width: 150rpx; color: $txt2; font-size: 28rpx; }
.ipt { flex: 1; color: #fff; font-size: 28rpx; }
.ipt.val { color: #fff; }
.ph { color: #6b6b73; }
.genders { display: flex; gap: 20rpx; }
.g { padding: 10rpx 36rpx; border: 1rpx solid $line; border-radius: 30rpx; color: $txt2; font-size: 26rpx; }
.g.on { border-color: $pink; color: $pink2; }
.save { margin-top: 40rpx; background: linear-gradient(90deg, $pink, $pink2); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 20rpx; padding: 20rpx 0; }
</style>
