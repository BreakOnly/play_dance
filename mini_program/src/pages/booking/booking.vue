<template>
  <view class="schedule-page safe-top safe-bottom">
    <view class="nav-row"><view class="round" @tap="goBookings">↗</view><view class="nav-title">约课</view><view class="round" @tap="goHome">⌂</view></view>
    <view class="search-box"><text>🔍</text><input placeholder="搜索歌名、老师、舞种" v-model="query" @input="filterSchedules" /></view>

    <view class="date-head">
      <view><text>{{ activeDateTitle }} ▸</text><view>{{ activeDateLabel }}</view></view>
      <view class="studio-filter">玩舞尚志 <text>未来15天可约</text></view>
    </view>

    <view class="date-strip" :class="{ expanded: calendarExpanded }">
      <view class="view-button" @tap="toggleCalendar">课表<text>{{ calendarExpanded ? '收起日历' : '展开日历' }}</text></view>
      <scroll-view v-if="!calendarExpanded" class="date-scroll" scroll-x enhanced :show-scrollbar="false"
        :scroll-into-view="'day-' + activeDate">
        <view class="dates">
          <view v-for="d in days" :key="d.date" :id="'day-' + d.date" class="date-item"
            :class="{ active: d.date === activeDate }" @tap="selectDate(d.date)">
            <text>{{ d.week }}</text><view>{{ d.day }}</view>
          </view>
        </view>
      </scroll-view>
      <view v-else class="calendar-grid">
        <view v-for="d in days" :key="d.date" class="calendar-day"
          :class="{ active: d.date === activeDate }" @tap="selectDate(d.date)">
          <text>{{ d.week }}</text><view>{{ d.month }}/{{ d.day }}</view>
        </view>
      </view>
      <view class="date-count" @tap="toggleCalendar">{{ calendarExpanded ? '⌃' : '⌄' }}<text>15天</text></view>
    </view>

    <view class="teacher-overview">
      <view class="overview-title">当天老师与课程</view>
      <block v-if="teacherOverview.length">
        <view class="teacher-row" v-for="t in teacherOverview" :key="t.name"><text>{{ t.name }}</text><view>{{ t.courses }}</view></view>
      </block>
      <view v-else class="teacher-empty">当天暂未排课</view>
    </view>

    <scroll-view class="dance-scroll" scroll-x enhanced :show-scrollbar="false">
      <view class="dance-filters">
        <view v-for="d in dances" :key="d" class="dance-filter" :class="{ active: d === activeDance }" @tap="selectDance(d)">{{ d }}</view>
      </view>
    </scroll-view>

    <view class="store-head"><view><text></text>玩舞尚志</view><text>{{ visibleSchedules.length }} 节课</text></view>
    <view v-if="loading" class="empty-card card">正在读取课表…</view>
    <view v-else-if="!visibleSchedules.length" class="empty-card card">没有匹配课程<text>换个日期、老师或舞种看看吧</text></view>
    <view v-else class="course-list">
      <view v-for="(s, i) in visibleSchedules" :key="s.id" class="schedule-card card" @tap="openCourse(s)">
        <view class="course-thumb"><text>{{ emojis[i % emojis.length] }}</text><view>{{ badges[i % badges.length] }}</view></view>
        <view class="schedule-body">
          <view class="schedule-title">{{ s.courseName }} <text>（{{ s.levelName }}）</text></view>
          <view class="teacher-line"><text>{{ s.teacherName }}</text> {{ fmtTime(s.startTime) }}–{{ fmtTime(s.endTime) }}</view>
          <view class="room-line">{{ s.room || '待定' }} · {{ s.durationMin || 60 }} 分钟</view>
          <view v-if="s.song" class="song-line">♫ {{ s.song }}</view>
          <view class="desc-line"><text>❗</text>{{ s.description || '基础律动和 step 连接练习' }}</view>
          <view class="card-foot">
            <text>{{ seatText(s) }}</text>
            <button class="book-button" :class="{ full: isFull(s) }" @tap.stop="onBook(s)">{{ btnText(s) }}</button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/api'

const emojis = ['🕺', '💃', '✨', '🔥']
const badges = ['零基础友好', '热门课程', '身体开发', '进阶提升']
const dances = ['全部', 'Hiphop', 'Kpop', 'Jazz', 'Urban', 'Breaking']

const query = ref('')
const activeDate = ref('')
const activeDateTitle = ref('Today')
const activeDateLabel = ref('')
const activeDance = ref('全部')
const calendarExpanded = ref(false)
const loading = ref(true)
const days = ref([])
const allSchedules = ref([])
const visibleSchedules = ref([])
const teacherOverview = ref([])

function buildDays() {
  const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const start = new Date(); start.setHours(0, 0, 0, 0)
  const arr = []
  for (let i = 0; i < 15; i++) {
    const dt = new Date(start); dt.setDate(start.getDate() + i)
    arr.push({
      date: fmtDate(dt), week: i === 0 ? '今天' : i === 1 ? '明天' : weekNames[dt.getDay()],
      day: dt.getDate(), month: dt.getMonth() + 1
    })
  }
  days.value = arr
  activeDate.value = arr[0].date
}

function fmtDate(dt) {
  return dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-' + String(dt.getDate()).padStart(2, '0')
}

function loadAll() {
  loading.value = true
  const first = days.value[0], last = days.value[days.value.length - 1]
  const end = new Date(last.date); end.setDate(end.getDate() + 1)
  api.schedules({ start: first.date + 'T00:00:00', end: fmtDate(end) + 'T00:00:00' }).then((list) => {
    allSchedules.value = (list || []).filter(s => s.startTime)
    filterSchedules()
  }).catch(() => {}).finally(() => { loading.value = false })
}

function filterSchedules() {
  const dayList = allSchedules.value.filter(s => s.startTime && s.startTime.startsWith(activeDate.value))
  let filtered = dayList
  if (activeDance.value !== '全部') {
    filtered = filtered.filter(s => (s.danceTypeName || s.courseType || '') === activeDance.value)
  }
  const q = query.value.trim().toLowerCase()
  if (q) {
    filtered = filtered.filter(s => {
      const txt = `${s.courseName} ${s.danceTypeName || ''} ${s.teacherName} ${s.song || ''}`.toLowerCase()
      return txt.includes(q)
    })
  }
  visibleSchedules.value = filtered

  // 老师概览
  const map = {}
  dayList.forEach(s => {
    const name = s.teacherName || '老师'
    if (!map[name]) map[name] = []
    map[name].push(`${fmtTime(s.startTime)} ${s.courseName}`)
  })
  teacherOverview.value = Object.keys(map).map(name => ({ name, courses: map[name].join(' · ') }))

  const idx = days.value.findIndex(d => d.date === activeDate.value)
  activeDateTitle.value = idx === 0 ? 'Today' : idx === 1 ? 'Tomorrow' : 'Class Day'
  activeDateLabel.value = activeDate.value ? activeDate.value.substring(5, 7).replace(/^0/, '') + '月' + activeDate.value.substring(8, 10).replace(/^0/, '') + '日' : ''
}

function selectDate(date) { activeDate.value = date; calendarExpanded.value = false; filterSchedules() }
function selectDance(d) { activeDance.value = d; filterSchedules() }
function toggleCalendar() { calendarExpanded.value = !calendarExpanded.value }

function remaining(s) { return Math.max(0, (s.capacity || 0) - (s.bookedCount || 0)) }
function isFull(s) { return remaining(s) === 0 }
function seatText(s) { const r = remaining(s); return r === 0 ? '已约满' : '剩 ' + r + ' 席' }
function btnText(s) {
  if (s.myBookingStatus === 'booked' || s.myBookingStatus === 'checked') return '已约'
  return isFull(s) ? '候补' : '预约'
}
function onBook(s) {
  if (s.myBookingStatus === 'booked' || s.myBookingStatus === 'checked') {
    uni.showToast({ title: '你已约该课', icon: 'none' }); return
  }
  const full = isFull(s)
  uni.showModal({
    title: full ? '加入候补' : '确认约课',
    content: `${s.courseName}\n${fmtTime(s.startTime)}–${fmtTime(s.endTime)}\n约课锁定 ${s.costHours || 1} 课时`,
    confirmColor: '#ff2d9b',
    success: (r) => {
      if (!r.confirm) return
      api.book(s.id).then(() => {
        uni.showToast({ title: full ? '已加入候补' : '约课成功', icon: 'none' }); loadAll()
      }).catch(() => {})
    }
  })
}
function openCourse(s) { uni.navigateTo({ url: '/pages/booking/detail?id=' + s.id }) }

function fmtTime(t) { if (!t) return ''; return t.substring(11, 16) }
function goBookings() { uni.navigateTo({ url: '/pages/mybookings/mybookings' }) }
function goHome() { uni.switchTab({ url: '/pages/home/home' }) }

onShow(() => {
  buildDays()
  const first = days.value[0], last = days.value[days.value.length - 1]
  const end = new Date(last.date); end.setDate(end.getDate() + 1)
  api.schedules({ start: first.date + 'T00:00:00', end: fmtDate(end) + 'T00:00:00' }).then((list) => {
    allSchedules.value = (list || []).filter(s => s.startTime)
    loading.value = false
    filterSchedules()
  }).catch(() => { loading.value = false })
})
</script>

<style lang="scss" scoped>
.schedule-page { min-height: 100vh; background: #0e0e0e; }
.nav-row { height: 88rpx; padding: 0 30rpx; display: flex; align-items: center; justify-content: space-between; }
.round { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #222226; color: #fff; font-size: 25rpx; }
.nav-title { font-size: 32rpx; font-weight: 900; }
.search-box { height: 76rpx; margin: 3rpx 30rpx 0; padding: 0 28rpx; display: flex; align-items: center; gap: 14rpx; border: 1rpx solid #2e2e33; border-radius: 38rpx; background: #1a1a1d; color: #6b6b73; }
.search-box input { min-width: 0; flex: 1; color: #fff; font-size: 23rpx; }
.date-head { padding: 34rpx 30rpx 15rpx; display: flex; align-items: flex-end; justify-content: space-between; }
.date-head > view:first-child > text { color: #ff6fb5; font-size: 38rpx; font-weight: 900; }
.date-head > view:first-child > view { margin-top: 5rpx; color: #fff; font-size: 23rpx; }
.studio-filter { max-width: 250rpx; color: #9a9aa2; font-size: 21rpx; text-align: right; }
.studio-filter text { margin-left: 8rpx; color: #ff6fb5; font-weight: 700; }
.date-strip { min-height: 128rpx; display: flex; align-items: stretch; border-bottom: 1rpx solid #2e2e33; }
.date-strip.expanded { min-height: 430rpx; }
.view-button { width: 116rpx; margin: 8rpx 10rpx 12rpx 24rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 1rpx solid #2e2e33; border-radius: 25rpx; background: #222226; font-size: 22rpx; }
.view-button text { display: block; color: #ff6fb5; }
.date-scroll { min-width: 0; flex: 1; }
.dates { height: 100%; display: flex; }
.date-item { position: relative; flex: 0 0 95rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #9a9aa2; }
.date-item text { font-size: 20rpx; }
.date-item view { margin-top: 7rpx; color: #9a9aa2; font-size: 31rpx; font-weight: 800; }
.date-item.active, .date-item.active view { color: #ff6fb5; }
.date-item.active::after { content: ''; position: absolute; bottom: 4rpx; width: 35rpx; height: 5rpx; border-radius: 4rpx; background: #ff2d9b; }
.date-count { width: 58rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #ff6fb5; font-size: 18rpx; }
.date-count text { margin-top: 4rpx; }
.calendar-grid { min-width: 0; flex: 1; padding: 13rpx 5rpx 16rpx; display: grid; grid-template-columns: repeat(5, 1fr); gap: 9rpx; }
.calendar-day { min-height: 92rpx; border: 1rpx solid #29292e; border-radius: 18rpx; background: #18181b; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #888891; }
.calendar-day text { font-size: 18rpx; }
.calendar-day view { margin-top: 5rpx; font-size: 22rpx; font-weight: 800; }
.calendar-day.active { border-color: #ff2d9b; background: rgba(255,45,155,.14); color: #ff6fb5; }
.teacher-overview { margin: 18rpx 30rpx 0; padding: 20rpx 22rpx; border: 1rpx solid #34343a; border-radius: 24rpx; background: #17171a; }
.overview-title { margin-bottom: 12rpx; color: #fff; font-size: 23rpx; font-weight: 800; }
.teacher-row { display: grid; grid-template-columns: 125rpx 1fr; gap: 12rpx; padding: 9rpx 0; border-top: 1rpx solid #29292e; align-items: center; }
.teacher-row > text { color: #ff6fb5; font-size: 21rpx; font-weight: 800; }
.teacher-row > view { color: #aaaab2; font-size: 19rpx; line-height: 1.5; }
.teacher-empty { color: #777780; font-size: 20rpx; }
.dance-scroll { width: 100%; margin-top: 18rpx; }
.dance-filters { display: flex; gap: 13rpx; padding: 0 30rpx; }
.dance-filter { flex: none; padding: 10rpx 23rpx; border-radius: 99rpx; background: #1a1a1d; color: #787880; font-size: 21rpx; }
.dance-filter.active { background: rgba(255,45,155,.14); color: #ff6fb5; }
.store-head { padding: 31rpx 30rpx 16rpx; display: flex; align-items: center; justify-content: space-between; }
.store-head > view { font-size: 29rpx; font-weight: 900; }
.store-head > view > text { width: 28rpx; height: 28rpx; margin-right: 16rpx; display: inline-block; border-radius: 7rpx; background: #ff2d9b; transform: rotate(45deg); vertical-align: -4rpx; }
.store-head > text { color: #ff6fb5; font-size: 20rpx; }
.course-list { padding: 0 30rpx; display: flex; flex-direction: column; gap: 20rpx; }
.schedule-card { overflow: hidden; display: flex; }
.course-thumb { position: relative; width: 215rpx; flex: none; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#3a3a40,#1c1c20); }
.course-thumb > text { font-size: 58rpx; }
.course-thumb > view { position: absolute; left: 0; bottom: 0; padding: 7rpx 15rpx; border-radius: 0 18rpx 0 0; background: #ff2d9b; color: #fff; font-size: 17rpx; font-weight: 700; }
.schedule-body { min-width: 0; flex: 1; padding: 24rpx; }
.schedule-title { color: #fff; font-size: 27rpx; font-weight: 900; }
.schedule-title text { color: #ff6fb5; }
.teacher-line { margin-top: 11rpx; color: #fff; font-size: 22rpx; }
.teacher-line text { margin-right: 10rpx; color: #ff6fb5; font-weight: 800; }
.room-line { margin-top: 5rpx; color: #9a9aa2; font-size: 20rpx; }
.song-line { margin-top: 11rpx; color: #b3a0ff; font-size: 20rpx; }
.desc-line { margin-top: 11rpx; overflow: hidden; display: -webkit-box; color: #9a9aa2; font-size: 20rpx; line-height: 1.45; -webkit-line-clamp: 3; -webkit-box-orient: vertical; }
.desc-line text { color: #ff2d9b; }
.card-foot { margin-top: 16rpx; display: flex; align-items: center; justify-content: flex-end; }
.card-foot > text { margin-right: auto; color: #ffd166; font-size: 18rpx; }
.book-button { width: 142rpx; height: 66rpx; margin: 0; display: flex; align-items: center; justify-content: center; border-radius: 22rpx; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); color: #fff; font-size: 24rpx; font-weight: 800; }
.book-button.full { border: 1rpx solid #2e2e33; background: #222226; color: #777780; }
.empty-card { margin: 0 30rpx; padding: 90rpx 20rpx; color: #aaaab2; text-align: center; }
.empty-card text { display: block; margin-top: 12rpx; color: #666670; font-size: 21rpx; }
</style>