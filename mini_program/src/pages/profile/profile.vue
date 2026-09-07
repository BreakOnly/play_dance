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
        <button class="wx-phone" open-type="getPhoneNumber" @getphonenumber="onGetPhone">一键绑定</button>
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
// 微信手机号授权（企业主体小程序生效；用户允许后 code 交后端换手机号）
function onGetPhone(e) {
  const d = e.detail || {}
  if (!d.code) {
    const denied = (d.errMsg || '').includes('deny')
    uni.showToast({ title: denied ? '已取消授权' : '当前环境不支持，请手动填写', icon: 'none' })
    return
  }
  api.bindPhone(d.code).then((phone) => {
    form.value.phone = phone
    uni.showToast({ title: '已绑定', icon: 'success' })
  }).catch(() => {})
}
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
.wx-phone { margin: 0; padding: 0 26rpx; height: 62rpx; line-height: 62rpx; flex-shrink: 0; border: 1rpx solid rgba(237,90,154,.55); border-radius: 31rpx; background: rgba(237,90,154,.08); color: #f484b4; font-size: 24rpx; font-weight: 600; }
.save { margin-top: 40rpx; background: linear-gradient(90deg, $pink, $pink2); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 20rpx; padding: 20rpx 0; }
</style>
