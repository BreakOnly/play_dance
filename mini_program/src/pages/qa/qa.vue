<template>
  <view class="consult-page safe-top">
    <view class="consult-head">
      <image src="/static/logo.png" mode="widthFix"></image>
      <view><text>咨询问答</text><text>玩舞尚志 · DANCE CASUALLY</text></view>
    </view>

    <view class="section-title first-title">
      <view>点赞排行</view>
      <text @tap="toggleRanking">{{ rankingExpanded ? '收起' : '更多' }} ›</text>
    </view>
    <view v-if="rankingExpanded" class="rank-search">
      <text>🔍</text><input v-model="rankQuery" placeholder="搜索视频或老师" @input="searchRanking" />
    </view>
    <view v-if="visibleRankings.length" class="ranking card">
      <view class="rank-item" v-for="(v, i) in visibleRankings" :key="v.id" @tap="play(v)">
        <text class="rank-number">{{ i + 1 }}</text>
        <view class="rank-cover">▶</view>
        <view class="rank-copy">
          <text>{{ v.title }}</text>
          <text>{{ v.authorName || '老师' }}</text>
          <view class="rank-meta">
            <text>{{ v.duration ? fmtDur(v.duration) : '' }}</text>
            <view class="like-button" :class="{ liked: v.liked }" @tap.stop="toggleLike(v)">♥ {{ fmtLikes(v.likeCount) }}</view>
          </view>
        </view>
        <text class="rank-arrow">›</text>
      </view>
    </view>
    <view v-else class="empty card">没有找到相关排行内容</view>

    <view class="section-title"><view>问答精选</view><text>{{ questions.length }} 条</text></view>
    <view class="qa-panel card">
      <view class="qa-card" v-for="q in questions" :key="q.id">
        <view><text>Q</text>{{ q.content }}</view>
        <text v-if="q.firstAnswer">{{ q.firstAnswer.content }}</text>
        <text v-if="q.firstAnswer">👩‍🏫 {{ q.firstAnswer.isTeacher ? (q.firstAnswer.teacherName || '老师') : (q.firstAnswer.authorName || '舞友') }} · 已解答</text>
      </view>
    </view>

    <view class="contact-card" @tap="callStudio">
      <view><text>还有问题？</text><text>新人试课、购课和课程安排请电话咨询</text></view>
      <view><text>☎</text><text>{{ servicePhone }}</text></view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/api'

const rankingExpanded = ref(false)
const rankQuery = ref('')
const allRankings = ref([])
const visibleRankings = ref([])
const questions = ref([])
const servicePhone = ref('')

onShow(() => {
  api.videoRanking(20).then((list) => {
    allRankings.value = list || []
    visibleRankings.value = allRankings.value.slice(0, 3)
  }).catch(() => {})
  api.qaQuestions(1, 10).then((d) => {
    questions.value = (d && d.list) || []
  }).catch(() => {})
  api.home().then((d) => {
    servicePhone.value = d.servicePhone || ''
  }).catch(() => {})
})

function toggleRanking() {
  rankingExpanded.value = !rankingExpanded.value
  rankQuery.value = ''
  visibleRankings.value = rankingExpanded.value ? allRankings.value : allRankings.value.slice(0, 3)
}

function searchRanking() {
  const q = rankQuery.value.trim().toLowerCase()
  visibleRankings.value = q
    ? allRankings.value.filter(v => (v.title + (v.authorName || '')).toLowerCase().includes(q))
    : allRankings.value
}

function toggleLike(v) {
  if (v.busy) return
  v.busy = true
  api.videoLike(v.id, !v.liked).then((d) => {
    v.liked = d.liked
    v.likeCount = d.likeCount
  }).catch(() => {}).finally(() => { v.busy = false })
}

function play(v) {
  api.videoPlayUrl(v.id).then((d) => {
    uni.navigateTo({ url: '/pages/video/player?id=' + v.id })
  }).catch(() => {})
}

function callStudio() {
  uni.showModal({ title: '联系舞房', content: servicePhone.value ? '请拨打电话：' + servicePhone.value : '门店暂未配置联系电话，请到店咨询。', showCancel: false, confirmColor: '#ff2d9b' })
}

function fmtDur(sec) {
  if (!sec) return ''
  const m = Math.floor(sec / 60), s = sec % 60
  return m + ':' + String(s).padStart(2, '0')
}
function fmtLikes(n) {
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return n
}
</script>

<style lang="scss" scoped>
.consult-page { min-height: 100vh; padding: 14rpx 28rpx calc(190rpx + env(safe-area-inset-bottom)); background: #0d0d0e; color: #fff; overflow: visible; }
.consult-head { min-height: 118rpx; display: flex; align-items: center; gap: 20rpx; }
.consult-head image { width: 145rpx; }
.consult-head > view { display: flex; flex-direction: column; }
.consult-head > view text:first-child { font-size: 38rpx; font-weight: 900; }
.consult-head > view text:last-child { margin-top: 6rpx; color: #ff5eab; font-size: 20rpx; }
.section-title { min-height: 86rpx; display: flex; align-items: center; justify-content: space-between; }
.first-title { margin-top: 2rpx; }
.section-title view { padding-left: 18rpx; position: relative; font-size: 31rpx; font-weight: 900; }
.section-title view::before { content: ''; position: absolute; left: 0; top: 4rpx; width: 7rpx; height: 30rpx; border-radius: 5rpx; background: #ff2694; }
.section-title > text { color: #ddd; font-size: 22rpx; }
.rank-search { height: 76rpx; margin-bottom: 17rpx; padding: 0 24rpx; border: 1rpx solid #36363d; border-radius: 38rpx; background: #1a1a1d; display: flex; align-items: center; gap: 15rpx; color: #818195; }
.rank-search input { min-width: 0; flex: 1; color: #fff; font-size: 23rpx; }
.ranking { overflow: hidden; }
.rank-item { min-height: 142rpx; padding: 18rpx; border-bottom: 1rpx solid #35353a; display: grid; grid-template-columns: 48rpx 92rpx 1fr 20rpx; align-items: center; gap: 15rpx; }
.rank-item:last-child { border-bottom: 0; }
.rank-number { color: #ff3c9e; text-align: center; font-size: 29rpx; font-weight: 900; }
.rank-cover { width: 92rpx; height: 92rpx; border-radius: 20rpx; background: linear-gradient(135deg,#351225,#1d151c); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28rpx; }
.rank-copy { min-width: 0; display: flex; flex-direction: column; }
.rank-copy text:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 27rpx; font-weight: 900; }
.rank-copy text:nth-child(2) { margin-top: 6rpx; color: #ff5eab; font-size: 20rpx; }
.rank-meta { margin-top: 7rpx; display: flex; align-items: center; gap: 18rpx; color: #aaaabd; font-size: 20rpx; }
.rank-meta > text { margin: 0 !important; color: #aaaabd !important; font-size: 20rpx !important; font-weight: 400 !important; }
.like-button { min-width: 92rpx; padding: 7rpx 14rpx; border: 1rpx solid #4c263c; border-radius: 22rpx; background: #2a1722; color: #aaaabd; text-align: center; }
.like-button.liked { border-color: #ff3c9e; background: #4a1633; color: #ff5eab; }
.rank-arrow { color: #777780; font-size: 28rpx; }
.qa-panel { padding: 20rpx; }
.qa-card { padding: 22rpx; border: 1rpx solid #38383e; border-radius: 23rpx; background: #242429; }
.qa-card + .qa-card { margin-top: 16rpx; }
.qa-card > view { font-size: 26rpx; font-weight: 800; }
.qa-card > view text { display: inline-flex; width: 42rpx; height: 42rpx; margin-right: 12rpx; align-items: center; justify-content: center; border-radius: 50%; color: #ff4aa4; background: #38132b; }
.qa-card > text { display: block; margin-top: 15rpx; color: #b3b3c1; font-size: 22rpx; line-height: 1.7; }
.qa-card > text:last-child { color: #858696; font-size: 19rpx; }
.empty { padding: 45rpx 25rpx; color: #aaaabd; text-align: center; font-size: 23rpx; }
.contact-card { margin-top: 28rpx; padding: 29rpx 26rpx; border-radius: 28rpx; background: linear-gradient(120deg,#ff2495,#8e0c55); display: flex; align-items: center; justify-content: space-between; }
.contact-card > view { display: flex; flex-direction: column; }
.contact-card > view:first-child text:first-child { font-size: 28rpx; font-weight: 900; }
.contact-card > view:first-child text:last-child { max-width: 390rpx; margin-top: 7rpx; font-size: 20rpx; color: rgba(255,255,255,.82); }
.contact-card > view:last-child { align-items: flex-end; gap: 5rpx; font-size: 21rpx; }
</style>
