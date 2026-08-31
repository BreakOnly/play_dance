<template>
  <view class="schedule-page safe-top safe-bottom">
    <view class="search-box">
      <view class="search-icon"><view class="lens"></view><view class="handle"></view></view>
      <input placeholder="搜索歌名、老师、舞种" v-model="query" @input="filterSchedules" />
    </view>

    <view class="date-head">
      <view><text>{{ activeDateTitle }} ▸</text><view>{{ activeDateLabel }}</view></view>
      <text class="brand-text">玩舞尚志</text>
    </view>

    <view class="date-strip">
      <view class="view-entry" @tap="goScheduleView"><text>课表</text><text>视图</text></view>
      <scroll-view class="date-scroll" scroll-x enhanced :show-scrollbar="false"
        :scroll-left="stripLeft" scroll-with-animation @scroll="onStripScroll">
        <view class="dates">
          <view v-for="d in days" :key="d.date" class="date-item"
            :class="{ active: d.date === activeDate }" @tap="selectDate(d.date)">
            <text>{{ d.week }}</text><view>{{ d.day }}</view>
          </view>
        </view>
      </scroll-view>
      <view class="date-count filter-entry" :class="{ active: hasFilter }" @tap="openFilter">
        <view class="funnel"></view>
      </view>
    </view>

    <view class="teacher-overview" v-if="teacherOverview.length">
      <view class="overview-title">当天老师与课程</view>
      <view class="teacher-row" v-for="t in teacherOverview" :key="t.name">
        <view class="ov-teacher">
          <image v-if="t.avatar" :src="fileUrl(t.avatar)" class="ov-avatar" mode="aspectFill" />
          <view v-else class="ov-avatar ph">{{ (t.name || 'T')[0] }}</view>
          <text>{{ t.name }}</text>
        </view>
        <view>{{ t.courses }}</view>
      </view>
    </view>

    <view class="store-head"><view><text></text>玩舞尚志</view><text>{{ visibleSchedules.length }} 节课</text></view>
    <view v-if="loading" class="empty-card card">正在读取课表…</view>
    <view v-else-if="!visibleSchedules.length" class="empty-card card">没有匹配课程<text>换个日期、老师或舞种看看吧</text></view>
    <view v-else class="course-list">
      <view v-for="(s, i) in visibleSchedules" :key="s.id" class="schedule-card card" @tap="openCourse(s)">
        <view class="course-thumb"><text>{{ emojis[i % emojis.length] }}</text><view>{{ badges[i % badges.length] }}</view></view>
        <view class="schedule-body">
          <view class="schedule-title">{{ s.courseName }} <text>（{{ s.levelName }}）</text></view>
          <view class="teacher-line">
            <image v-if="s.teacherAvatar" :src="fileUrl(s.teacherAvatar)" class="t-avatar" mode="aspectFill" />
            <view v-else class="t-avatar ph">{{ (s.teacherName || 'T')[0] }}</view>
            <text class="t-name">{{ s.teacherName }}</text>
            <text class="t-time">{{ fmtTime(s.startTime) }}–{{ fmtTime(s.endTime) }}</text>
          </view>
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

    <!-- 筛选弹层 -->
    <view class="flt-mask" v-if="fltVisible" @tap="closeFilter">
      <view class="flt-panel" @tap.stop>
        <view class="flt-tabs">
          <view v-for="t in fltTabs" :key="t.key" class="flt-tab" :class="{ on: fltTab === t.key }" @tap="fltTab = t.key">{{ t.label }}</view>
        </view>

        <!-- 课程：左侧分类(舞种/难易/类型) + 右侧两列标签 -->
        <view v-if="fltTab === 'course'" class="flt-body">
          <scroll-view class="flt-side" scroll-y enhanced :show-scrollbar="false">
            <view v-for="c in sideCats" :key="c.key" class="flt-side-item" :class="{ on: fltSide === c.key }" @tap="fltSide = c.key">{{ c.label }}</view>
          </scroll-view>
          <scroll-view class="flt-opts" scroll-y enhanced :show-scrollbar="false">
            <view class="flt-grid">
              <view v-for="o in sideOptions" :key="String(o.value)" class="flt-tag" :class="{ on: draft[fltSideKey].includes(o.value) }" @tap="pick(fltSideKey, o.value)">{{ o.label }}</view>
            </view>
          </scroll-view>
        </view>

        <!-- 时段：固定选项 -->
        <view v-else-if="fltTab === 'time'" class="flt-body">
          <scroll-view class="flt-opts full" scroll-y enhanced :show-scrollbar="false">
            <view class="flt-grid">
              <view v-for="t in timeSlots" :key="t.value" class="flt-tag" :class="{ on: draft.time.includes(t.value) }" @tap="pick('time', t.value)">{{ t.label }}</view>
            </view>
          </scroll-view>
        </view>

        <!-- 老师：后端接口返回 -->
        <view v-else class="flt-body">
          <scroll-view class="flt-opts full" scroll-y enhanced :show-scrollbar="false">
            <view class="flt-grid">
              <view v-for="t in fltData.teachers || []" :key="t.id" class="flt-tag" :class="{ on: draft.teacher.includes(t.id) }" @tap="pick('teacher', t.id)">{{ t.name }}</view>
            </view>
          </scroll-view>
        </view>

        <view class="flt-foot">
          <view class="flt-reset" @tap="resetFilter">重置</view>
          <view class="flt-ok" @tap="applyFilter">确认</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReady } from '@dcloudio/uni-app'
import { api, fileUrl } from '@/api'

const emojis = ['🕺', '💃', '✨', '🔥']
const badges = ['零基础友好', '热门课程', '身体开发', '进阶提升']
// 中文星期（左上角日期标签用）
const WEEK_CN = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

// 固定时段（其余筛选项走后端接口）
const timeSlots = [
  { value: 'am', label: '上午 8:00-11:00', from: 8, to: 12 },
  { value: 'noon', label: '中午12:00-14:00', from: 12, to: 15 },
  { value: 'pm', label: '下午15:00-18:00', from: 15, to: 19 },
  { value: 'eve', label: '晚上19:00-20:00', from: 19, to: 21 },
  { value: 'night', label: '深夜21:00-23:00', from: 21, to: 24 }
]
const fltTabs = [
  { key: 'course', label: '课程' },
  { key: 'time', label: '时段' },
  { key: 'teacher', label: '老师' }
]
const sideCats = [
  { key: 'danceType', label: '舞种' },
  { key: 'level', label: '难易' },
  { key: 'courseType', label: '类型' }
]

const query = ref('')
const activeDate = ref('')
const activeDateTitle = ref('Today')
const activeDateLabel = ref('')
const loading = ref(true)
const days = ref([])
const allSchedules = ref([])
const visibleSchedules = ref([])
const teacherOverview = ref([])

// ===== 筛选弹层（多选） =====
const fltVisible = ref(false)
const fltTab = ref('course')
const fltSide = ref('danceType')
const fltData = ref({})
const fltLoaded = ref(false)
const emptySel = () => ({ danceType: [], level: [], courseType: [], time: [], teacher: [] })
const copySel = (sel) => ({ danceType: [...sel.danceType], level: [...sel.level], courseType: [...sel.courseType], time: [...sel.time], teacher: [...sel.teacher] })
const draft = ref(emptySel())
const applied = ref(emptySel())

const fltSideKey = computed(() => fltSide.value)
const sideOptions = computed(() => {
  if (fltSide.value === 'danceType') return (fltData.value.danceTypes || []).map(d => ({ value: d.id, label: d.name }))
  if (fltSide.value === 'level') return (fltData.value.levels || []).map(l => ({ value: l.id, label: l.name }))
  return (fltData.value.courseTypes || []).map(t => ({ value: t.value, label: t.label }))
})
const hasFilter = computed(() => Object.values(applied.value).some(arr => arr.length > 0))

function openFilter() {
  draft.value = copySel(applied.value)
  fltVisible.value = true
  if (!fltLoaded.value) {
    api.scheduleFilters().then((d) => { fltData.value = d || {}; fltLoaded.value = true }).catch(() => {})
  }
}
function closeFilter() { fltVisible.value = false }
function pick(key, value) {
  const arr = draft.value[key]
  const i = arr.indexOf(value)
  if (i >= 0) arr.splice(i, 1)
  else arr.push(value)
}
function resetFilter() {
  draft.value = emptySel()
  applied.value = emptySel()
  filterSchedules()
}
function applyFilter() {
  applied.value = copySel(draft.value)
  fltVisible.value = false
  filterSchedules()
}

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
  const q = query.value.trim().toLowerCase()
  if (q) {
    filtered = filtered.filter(s => {
      const txt = `${s.courseName} ${s.danceTypeName || ''} ${s.teacherName} ${s.song || ''}`.toLowerCase()
      return txt.includes(q)
    })
  }

  // 筛选面板条件（各组内多选，命中任一即可）
  const f = applied.value
  if (f.danceType.length) filtered = filtered.filter(s => f.danceType.includes(s.danceTypeId))
  if (f.level.length) filtered = filtered.filter(s => f.level.includes(s.levelId))
  if (f.courseType.length) filtered = filtered.filter(s => f.courseType.includes(s.courseType))
  if (f.time.length) {
    const slots = timeSlots.filter(t => f.time.includes(t.value))
    filtered = filtered.filter(s => {
      const h = parseInt((s.startTime || '').substring(11, 13), 10)
      return slots.some(slot => h >= slot.from && h < slot.to)
    })
  }
  if (f.teacher.length) filtered = filtered.filter(s => f.teacher.includes(s.teacherId))

  visibleSchedules.value = filtered

  // 老师概览（同一老师取第一个非空头像）
  const map = {}
  dayList.forEach(s => {
    const name = s.teacherName || '老师'
    if (!map[name]) map[name] = { courses: [], avatar: '' }
    if (!map[name].avatar && s.teacherAvatar) map[name].avatar = s.teacherAvatar
    map[name].courses.push(`${fmtTime(s.startTime)} ${s.courseName}`)
  })
  teacherOverview.value = Object.keys(map).map(name => ({ name, avatar: map[name].avatar, courses: map[name].courses.join(' · ') }))

  const idx = days.value.findIndex(d => d.date === activeDate.value)
  const weekCn = activeDate.value ? WEEK_CN[new Date(activeDate.value + 'T00:00:00').getDay()] : ''
  // 左上角：仅今天显示 Today，其余显示当前年份；下行 日期 + 周几
  activeDateTitle.value = idx === 0 ? 'Today' : String(new Date().getFullYear())
  activeDateLabel.value = activeDate.value ? (activeDate.value.substring(5, 7).replace(/^0/, '') + '月' + activeDate.value.substring(8, 10).replace(/^0/, '') + '日') + ' ' + weekCn : ''
}

function selectDate(date) {
  activeDate.value = date
  // 点击可视区最后两个日期时，日期条向右平移一天；其余情况不滚动
  const itemW = uni.upx2px(95)
  if (stripWidth > 0 && itemW > 0) {
    const idx = days.value.findIndex(d => d.date === date)
    const firstVisible = Math.round(stripRealLeft / itemW)
    const visibleCount = Math.max(3, Math.floor(stripWidth / itemW))
    if (idx - firstVisible >= visibleCount - 2) {
      const max = Math.max(0, days.value.length * itemW - stripWidth)
      stripLeft.value = Math.min(stripRealLeft + itemW, max)
    }
  }
  filterSchedules()
}

// ===== 日期条滚动（点击右侧日期时整体右移一天，不做定位跳转） =====
const stripLeft = ref(0)      // 绑定 scroll-left（仅程序赋值，带动画）
let stripRealLeft = 0         // 用户手势实时位置
let stripWidth = 0            // 日期条可视宽度

function onStripScroll(e) { stripRealLeft = e.detail.scrollLeft }

function measureStrip() {
  uni.createSelectorQuery().select('.date-scroll').boundingClientRect((r) => { if (r) stripWidth = r.width }).exec()
}
// 进入课表视图（日/周视图切换页）
function goScheduleView() { uni.navigateTo({ url: '/pages/scheduleview/scheduleview' }) }

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

onReady(measureStrip)
onShow(() => {
  measureStrip()
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
.search-box { height: 76rpx; margin: 3rpx 30rpx 0; padding: 0 28rpx; display: flex; align-items: center; gap: 14rpx; border: 1rpx solid #2e2e33; border-radius: 38rpx; background: #1a1a1d; color: #6b6b73; }
.search-box input { min-width: 0; flex: 1; color: #fff; font-size: 23rpx; }
.search-icon { position: relative; width: 30rpx; height: 30rpx; flex: none; }
.search-icon .lens { position: absolute; left: 0; top: 0; width: 22rpx; height: 22rpx; border: 2rpx solid #6b6b73; border-radius: 50%; }
.search-icon .handle { position: absolute; right: 0; bottom: 1rpx; width: 12rpx; height: 2rpx; background: #6b6b73; transform: rotate(45deg); }
.date-head { padding: 34rpx 30rpx 15rpx; display: flex; align-items: flex-end; justify-content: space-between; }
.date-head > view:first-child > text { color: #ff6fb5; font-size: 38rpx; font-weight: 900; }
.date-head > view:first-child > view { margin-top: 5rpx; color: #fff; font-size: 23rpx; }
.brand-text { color: #ff5fae; font-size: 25rpx; font-weight: 900; letter-spacing: 3rpx; text-shadow: 0 0 14rpx rgba(255,45,155,.5); }
.date-strip { min-height: 128rpx; display: flex; align-items: stretch; border-bottom: 1rpx solid #2e2e33; }
/* 课表视图入口（Blackpink 风：黑底 + 荧光粉） */
.view-entry { width: 116rpx; margin: 8rpx 10rpx 12rpx 24rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2rpx; border: 1rpx solid rgba(255,45,155,.55); border-radius: 25rpx; background: linear-gradient(135deg,#2b1322,#1a1a1e); box-shadow: 0 0 18rpx rgba(255,45,155,.22); color: #ff5fae; font-size: 25rpx; font-weight: 900; letter-spacing: 2rpx; text-shadow: 0 0 12rpx rgba(255,45,155,.5); }
.date-scroll { min-width: 0; flex: 1; }
.dates { height: 100%; display: flex; }
.date-item { position: relative; flex: 0 0 95rpx; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; padding: 18rpx 0 30rpx; color: #9a9aa2; }
.date-item text { font-size: 20rpx; }
.date-item view { margin-top: 7rpx; color: #9a9aa2; font-size: 31rpx; font-weight: 800; }
.date-item.active, .date-item.active view { color: #ff6fb5; }
.date-item.active::after { content: ''; position: absolute; bottom: 12rpx; width: 35rpx; height: 5rpx; border-radius: 4rpx; background: #ff2d9b; }
/* 筛选按钮：仅漏斗图标；底色与日期区一致（透明），靠描边辨识 */
.filter-entry { margin: 8rpx 20rpx 12rpx 0; padding: 0 40rpx; display: flex; align-items: center; justify-content: center; border: 1rpx solid #34343a; border-radius: 25rpx; background: transparent; color: #ff6fb5; }
.filter-entry.active { border-color: #ff2d9b; background: rgba(255,45,155,.16); }
.funnel { position: relative; width: 24rpx; height: 20rpx; }
.funnel::before { content: ''; position: absolute; left: 0; top: 0; border-left: 12rpx solid transparent; border-right: 12rpx solid transparent; border-top: 13rpx solid currentColor; }
.funnel::after { content: ''; position: absolute; left: 50%; bottom: 0; width: 5rpx; height: 7rpx; margin-left: -2.5rpx; background: currentColor; }
/* ===== 筛选弹层 ===== */
.flt-mask { position: fixed; inset: 0; z-index: 999; background: rgba(0,0,0,.55); display: flex; align-items: flex-end; }
.flt-panel { width: 100%; height: 78vh; border-radius: 32rpx 32rpx 0 0; background: #17171a; display: flex; flex-direction: column; animation: sheet-up .25s ease-out; overflow: hidden; }
.flt-tabs { display: flex; border-bottom: 1rpx solid #29292e; }
.flt-tab { flex: 1; height: 92rpx; display: flex; align-items: center; justify-content: center; color: #9a9aa2; font-size: 27rpx; position: relative; }
.flt-tab.on { color: #ff6fb5; font-weight: 800; }
.flt-tab.on::after { content: ''; position: absolute; bottom: 0; width: 56rpx; height: 6rpx; border-radius: 4rpx; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); }
.flt-body { min-height: 0; flex: 1; display: flex; }
.flt-side { width: 176rpx; flex: none; background: #131316; }
.flt-side-item { padding: 30rpx 24rpx; color: #9a9aa2; font-size: 23rpx; position: relative; }
.flt-side-item.on { color: #ff6fb5; font-weight: 800; background: #17171a; }
.flt-side-item.on::before { content: ''; position: absolute; left: 0; top: 26rpx; bottom: 26rpx; width: 6rpx; border-radius: 4rpx; background: #ff2d9b; }
.flt-opts { min-width: 0; flex: 1; }
.flt-opts.full { background: #17171a; }
.flt-grid { padding: 26rpx; display: grid; grid-template-columns: repeat(2, 1fr); gap: 18rpx; }
.flt-tag { min-height: 68rpx; padding: 0 16rpx; border: 1rpx solid #2e2e33; border-radius: 16rpx; background: #1c1c20; color: #c9c9d1; font-size: 22rpx; display: flex; align-items: center; justify-content: center; text-align: center; overflow: hidden; }
.flt-tag.on { border-color: #ff2d9b; background: rgba(255,45,155,.16); color: #ff6fb5; font-weight: 700; }
.flt-foot { padding: 20rpx 30rpx calc(24rpx + env(safe-area-inset-bottom)); display: flex; gap: 20rpx; border-top: 1rpx solid #29292e; }
.flt-reset { width: 180rpx; height: 84rpx; border: 1rpx solid #2e2e33; border-radius: 42rpx; background: #222226; color: #c9c9d1; font-size: 26rpx; display: flex; align-items: center; justify-content: center; }
.flt-ok { min-width: 0; flex: 1; height: 84rpx; border-radius: 42rpx; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); box-shadow: 0 8rpx 26rpx rgba(255,45,155,.38); color: #fff; font-size: 28rpx; font-weight: 800; letter-spacing: 2rpx; display: flex; align-items: center; justify-content: center; }
@keyframes sheet-up { from { transform: translateY(100%); } to { transform: translateY(0); } }
.teacher-overview { margin: 18rpx 30rpx 0; padding: 20rpx 22rpx; border: 1rpx solid #34343a; border-radius: 24rpx; background: #17171a; }
.overview-title { margin-bottom: 12rpx; color: #fff; font-size: 23rpx; font-weight: 800; }
.teacher-row { display: grid; grid-template-columns: 200rpx 1fr; gap: 12rpx; padding: 9rpx 0; border-top: 1rpx solid #29292e; align-items: center; }
.ov-teacher { display: flex; align-items: center; gap: 10rpx; }
.ov-teacher > text { color: #ff6fb5; font-size: 21rpx; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ov-avatar { width: 52rpx; height: 52rpx; border-radius: 50%; flex: none; background: #26262b; }
.ov-avatar.ph { display: flex; align-items: center; justify-content: center; color: #ff6fb5; font-size: 22rpx; font-weight: 800; }
.teacher-row > view { color: #aaaab2; font-size: 19rpx; line-height: 1.5; }
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
.teacher-line { margin-top: 11rpx; display: flex; align-items: center; color: #fff; font-size: 22rpx; }
.t-avatar { width: 40rpx; height: 40rpx; margin-right: 10rpx; border-radius: 50%; flex: none; background: #26262b; }
.t-avatar.ph { display: flex; align-items: center; justify-content: center; color: #ff6fb5; font-size: 20rpx; font-weight: 800; }
.teacher-line .t-name { margin-right: 10rpx; color: #ff6fb5; font-weight: 800; }
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