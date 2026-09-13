<template>
  <view class="page-shell"><video v-if="url" :src="url" controls autoplay style="width:100%" @error="onError" /><view v-if="error">{{ error }}</view><button v-if="error" @tap="load">重新获取播放地址</button></view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
const url = ref(''), error = ref(''), id = ref('')
onLoad(o => { id.value = o.id; load() })
async function load() { error.value = ''; try { const d = await api.videoPlayUrl(id.value); url.value = d.url } catch (e) { error.value = e?.message || '视频暂不可播放' } }
function onError() { error.value = '播放失败，地址可能已过期，请重试。' }
</script>
