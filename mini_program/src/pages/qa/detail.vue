<template>
  <scroll-view scroll-y class="page">
    <!-- 问题 -->
    <view class="q card">
      <view class="qhead">
        <view class="avatar">{{ (question.authorName || '舞')[0] }}</view>
        <view class="grow">
          <view class="qname">{{ question.authorName || '匿名舞者' }}</view>
          <view class="qtime">{{ fmtAgo(question.createdAt) }}</view>
        </view>
      </view>
      <view class="qcontent">{{ question.content }}</view>
      <view v-if="imgs.length" class="qimgs">
        <image v-for="(im, i) in imgs" :key="i" :src="fileUrl(im)" mode="aspectFill" class="qimg" @tap="preview(i)" />
      </view>
      <view class="qfoot">
        <text class="qact">💬 {{ answers.length }} 回答</text>
        <text class="qact" :class="{ liked: question.liked }" @tap="likeQuestion">👍 {{ question.likeCount }} 赞</text>
      </view>
    </view>

    <!-- 回答列表 -->
    <view class="sec-h"><text class="t">全部回答</text></view>
    <view v-for="a in answers" :key="a.id" class="acard card">
      <view class="ahead">
        <view class="avatar" :class="{ teacher: a.isTeacher }">{{ (aName(a) || '舞')[0] }}</view>
        <view class="grow">
          <view class="aname">
            {{ aName(a) }}
            <text v-if="a.isTeacher" class="badge-t">老师</text>
          </view>
          <view class="atime">{{ fmtAgo(a.createdAt) }}</view>
        </view>
      </view>
      <view class="acontent">{{ a.content }}</view>
      <view class="afoot">
        <text class="qact" :class="{ liked: a.liked }" @tap="likeAnswer(a)">👍 {{ a.likeCount }} 赞</text>
      </view>
    </view>
    <view v-if="!answers.length" class="empty">还没有回答，来抢沙发</view>

    <view style="height: 160rpx;"></view>

    <!-- 写回答 -->
    <view class="answer-bar">
      <input v-model="myAnswer" class="a-input" placeholder="写回答，帮助这位舞友…" confirm-type="send" @confirm="submitAnswer" />
      <button class="btn-primary send" :disabled="!myAnswer.trim()" @tap="submitAnswer">发送</button>
    </view>
  </scroll-view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api, fileUrl } from '@/api'

const question = ref({})
const answers = ref([])
const myAnswer = ref('')
const qid = ref(null)

const imgs = computed(() => {
  const im = question.value.images
  if (Array.isArray(im)) return im
  if (typeof im === 'string' && im.startsWith('[')) {
    try { return JSON.parse(im) } catch (e) { return [] }
  }
  return []
})

onLoad((opts) => {
  qid.value = opts.id
  load()
})

function load() {
  api.qaDetail(qid.value).then((d) => {
    question.value = d.question || {}
    answers.value = d.answers || []
  }).catch(() => {})
}

function aName(a) {
  return a.isTeacher ? (a.teacherName || '老师') : (a.authorName || '舞友')
}

function likeQuestion() {
  api.qaLike('question', qid.value).then((d) => {
    question.value.liked = d.liked
    question.value.likeCount = d.likeCount
  }).catch(() => {})
}

function likeAnswer(a) {
  api.qaLike('answer', a.id).then((d) => {
    a.liked = d.liked
    a.likeCount = d.likeCount
  }).catch(() => {})
}

function submitAnswer() {
  if (!myAnswer.value.trim()) return
  api.qaAnswer(qid.value, myAnswer.value.trim()).then(() => {
    myAnswer.value = ''
    uni.showToast({ title: '回答成功', icon: 'none' })
    load()
  }).catch(() => {})
}

function preview(i) {
  uni.previewImage({ urls: imgs.value.map(fileUrl), current: i })
}

function fmtAgo(t) {
  if (!t) return ''
  const diff = (Date.now() - new Date(t.replace(/-/g, '/')).getTime()) / 1000
  if (diff < 3600) return Math.max(1, Math.floor(diff / 60)) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 86400 * 30) return Math.floor(diff / 86400) + '天前'
  return t.substring(0, 10)
}
</script>

<style lang="scss" scoped>
.page { height: 100vh; box-sizing: border-box; background: $bg; padding-bottom: 40rpx; }
.q { margin: 24rpx; padding: 26rpx; }
.qhead { display: flex; align-items: center; gap: 16rpx; }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; background: linear-gradient(135deg, $pink, $pink2); display: flex; align-items: center; justify-content: center; font-weight: 900; flex-shrink: 0; }
.avatar.teacher { background: linear-gradient(135deg, #2ea043, #7ee787); }
.qname { font-size: 27rpx; font-weight: 700; }
.qtime { color: $txt3; font-size: 22rpx; margin-top: 4rpx; }
.qstage { color: $pink2; }
.qcontent { font-size: 29rpx; line-height: 1.75; margin-top: 18rpx; }
.qimgs { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 18rpx; }
.qimg { width: 200rpx; height: 200rpx; border-radius: 12rpx; background: $card2; }
.qfoot { display: flex; gap: 36rpx; margin-top: 20rpx; }
.qact { color: $txt2; font-size: 24rpx; }
.qact.liked { color: $pink2; }
.sec-h { padding: 16rpx 24rpx; }
.sec-h .t { font-size: 30rpx; font-weight: 800; border-left: 6rpx solid $pink; padding-left: 14rpx; }
.acard { margin: 0 24rpx 18rpx; padding: 24rpx; }
.ahead { display: flex; align-items: center; gap: 16rpx; }
.aname { font-size: 26rpx; font-weight: 700; }
.badge-t { background: rgba(126,231,135,.15); color: #7ee787; font-size: 19rpx; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 8rpx; font-weight: 400; }
.atime { color: $txt3; font-size: 21rpx; margin-top: 4rpx; }
.acontent { font-size: 26rpx; line-height: 1.7; margin-top: 14rpx; }
.afoot { margin-top: 14rpx; }
.empty { color: $txt3; font-size: 26rpx; text-align: center; padding: 50rpx 0; }
.answer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; gap: 16rpx; align-items: center; padding: 16rpx 24rpx calc(env(safe-area-inset-bottom) + 16rpx); background: $card; border-top: 1rpx solid $line; }
.a-input { flex: 1; background: $card2; border-radius: 999rpx; padding: 16rpx 28rpx; font-size: 26rpx; }
.send { font-size: 26rpx; padding: 0 36rpx; line-height: 2.4; }
.send[disabled] { opacity: .4; color: #fff; }
</style>
