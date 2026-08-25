<template>
  <!-- 承接 banner 的 url 类跳转；目标域名需在微信公众平台配置业务域名 -->
  <web-view :src="src"></web-view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const src = ref('')

onLoad((options) => {
  src.value = options.url ? decodeURIComponent(options.url) : ''
})
</script>
