<template>
  <view class="sv-page safe-top safe-bottom">
    <!-- 视图切换 + 筛选 / 当前日期 -->
    <view class="sv-toolbar">
      <view class="sv-tabs">
        <view class="sv-tab" :class="{ on: viewMode === 'day' }" @tap="viewMode = 'day'">日视图</view>
        <view class="sv-tab" :class="{ on: viewMode === 'week' }" @tap="viewMode = 'week'">周视图</view>
      </view>
      <view class="sv-right">
        <text class="sv-filter">筛选 ▾</text>
        <text class="sv-curdate">{{ viewMode === 'week' ? '本周' : activeDateLabel }} ▾</text>
      </view>
    </view>

    <!-- 日期条（从昨天起横向滚动），选中日期平滑滚动到居中（与门店课程页同款实现） -->
    <scroll-view class="sv-date-scroll" scroll-x enhanced :show-scrollbar="false"
      :scroll-left="stripLeft" scroll-with-animation>
      <view class="sv-dates">
        <view v-for="d in days" :key="d.date" :id="'svday-' + d.date" class="sv-date-item"
          :class="{ active: d.date === activeDate }" @tap="selectDate(d.date)">
          <text>{{ d.week }}</text><view>{{ d.day }}</view>
        </view>
      </view>
    </scroll-view>

    <!-- 日视图：待下一版实现 -->
    <view v-if="viewMode === 'day'" class="sv-body">
      <view class="sv-empty">日视图建设中，敬请期待</view>
    </view>

    <!-- 周视图 -->
    <view v-else>
      <view class="wv-actions">
        <text class="wv-gen" @tap="generateImage">生成课表图片</text>
      </view>

      <view v-if="weekLoading" class="sv-empty wv-pad">正在加载本周课表…</view>
      <view v-else class="wv-wrap">
        <!-- 表头：月份 + 7 天 -->
        <view class="wv-headrow">
          <view class="wv-month">{{ weekMonth }}月</view>
          <view v-for="d in weekDays" :key="d.date" class="wv-dayhead" :class="{ today: d.date === todayStr }">
            <text>周{{ d.cn }}</text><view>{{ d.day }}日</view>
          </view>
        </view>
        <!-- 时段行：上午 / 中午 / 下午 / 晚上 -->
        <view v-for="seg in weekSegments" :key="seg.key" class="wv-segrow">
          <view class="wv-seglabel"><text v-for="ch in seg.label" :key="ch">{{ ch }}</text></view>
          <view class="wv-cells">
            <view v-for="(cell, ci) in seg.cells" :key="ci" class="wv-cell">
              <view v-for="c in cell" :key="c.id" class="wv-card" @tap="openCourse(c)">
                <view class="wv-card-top">
                  <image v-if="c.teacherAvatar" :src="fileUrl(c.teacherAvatar)" class="wv-avatar" mode="aspectFill" />
                  <view v-else class="wv-avatar ph">{{ (c.teacherName || 'T')[0] }}</view>
                  <text class="wv-tname">{{ c.teacherName || '老师' }}</text>
                </view>
                <view class="wv-card-body">
                  <text class="wv-dance">{{ c.danceTypeName || c.courseName }}</text>
                  <text class="wv-time">{{ fmtHM(c.startTime) }}-{{ fmtHM(c.endTime) }}</text>
                  <text class="wv-meta">{{ courseTypeLabel(c) }}｜{{ c.levelName || '常规' }}</text>
                  <text class="wv-badge" :class="{ over: isOver(c) }">{{ seatText(c) }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 课表图片预览弹层 -->
    <view v-if="imgVisible" class="img-mask">
      <view class="img-close" @tap="imgVisible = false">✕</view>
      <scroll-view class="img-scroll" scroll-y>
        <view class="img-pic-wrap">
          <image v-if="imgPath" :src="imgPath" mode="widthFix" class="img-pic" @longpress="saveImage" />
        </view>
      </scroll-view>
      <view class="img-save" @tap="saveImage">保存</view>
    </view>

    <!-- 离屏画布：用于生成课表图片 -->
    <canvas type="2d" id="svCanvas" class="sv-canvas"></canvas>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { api, fileUrl } from '@/api'

// 当前视图模式：day=日视图 week=周视图
const viewMode = ref('day')
const activeDate = ref('')
const stripLeft = ref(0)
const days = ref([])

// 英文星期缩写（参考图二：昨天 / 今天 / MON TUE…）
const WEEK = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
// 中文星期（周视图表头，周一为一周开始）
const WEEK_CN = ['日', '一', '二', '三', '四', '五', '六']
// 时段划分（与约课页筛选一致）
const SEGMENTS = [
  { key: 'am', label: '上午', min: 0, max: 12 },
  { key: 'noon', label: '中午', min: 12, max: 15 },
  { key: 'pm', label: '下午', min: 15, max: 19 },
  { key: 'eve', label: '晚上', min: 19, max: 24 }
]

function buildDays() {
  // 从昨天起连排 15 天，默认选中今天
  const start = new Date(); start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - 1)
  const arr = []
  for (let i = 0; i < 15; i++) {
    const dt = new Date(start); dt.setDate(start.getDate() + i)
    arr.push({
      date: fmtDate(dt),
      week: i === 0 ? '昨天' : i === 1 ? '今天' : WEEK[dt.getDay()],
      day: dt.getDate()
    })
  }
  days.value = arr
  activeDate.value = arr[1].date
  centerDate(arr[1].date)
}

/** 选中日期滚动到日期条中间（每格 118rpx 无间隙；scroll-view 通栏宽度即屏宽） */
function centerDate(date) {
  const idx = days.value.findIndex((d) => d.date === date)
  if (idx < 0) return
  const win = uni.getSystemInfoSync()
  const itemW = 118 * win.windowWidth / 750
  stripLeft.value = Math.max(0, Math.round(idx * itemW + itemW / 2 - win.windowWidth / 2))
}

function selectDate(date) {
  activeDate.value = date
  centerDate(date)
}

function fmtDate(dt) {
  return dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-' + String(dt.getDate()).padStart(2, '0')
}

// 右上角日期标签（MM.DD）
const activeDateLabel = computed(() => activeDate.value ? activeDate.value.substring(5).replace('-', '.') : '')

// 今天（YYYY-MM-DD，用于周视图高亮当天表头）
const todayStr = fmtDate(new Date())

// ==================== 周视图 ====================
const weekLoading = ref(false)
const weekCourses = ref([])
const weekStart = ref('')   // 本周一（YYYY-MM-DD）

/** 求某日期所在周的周一 */
function mondayOf(dateStr) {
  const dt = new Date(dateStr + 'T00:00:00')
  const dow = dt.getDay() === 0 ? 7 : dt.getDay()
  dt.setDate(dt.getDate() - (dow - 1))
  return fmtDate(dt)
}

function loadWeek() {
  weekStart.value = mondayOf(activeDate.value)
  const start = new Date(weekStart.value + 'T00:00:00')
  const end = new Date(start); end.setDate(start.getDate() + 7)
  weekLoading.value = true
  api.schedules({ start: weekStart.value + 'T00:00:00', end: fmtDate(end) + 'T00:00:00' }).then((list) => {
    weekCourses.value = list || []
  }).catch(() => {}).finally(() => { weekLoading.value = false })
}
watch(activeDate, loadWeek)

// 周视图 7 天表头
const weekDays = computed(() => {
  if (!weekStart.value) return []
  const start = new Date(weekStart.value + 'T00:00:00')
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(start); d.setDate(start.getDate() + i)
    return { date: fmtDate(d), cn: WEEK_CN[d.getDay()], day: d.getDate() }
  })
})
// 表头月份（取周一所在月）
const weekMonth = computed(() => weekStart.value ? Number(weekStart.value.substring(5, 7)) : '')

// 按时段 × 星期聚合课程
const weekSegments = computed(() => {
  return SEGMENTS.map(seg => ({
    ...seg,
    cells: weekDays.value.map(d => weekCourses.value
      .filter(c => (c.startTime || '').substring(0, 10) === d.date)
      .filter(c => { const h = hourOf(c.startTime); return h >= seg.min && h < seg.max })
      .sort((a, b) => (a.startTime || '').localeCompare(b.startTime || '')))
  }))
})

function hourOf(t) { return parseInt((t || '').substring(11, 13), 10) || 0 }
function fmtHM(t) { return t ? t.substring(11, 16) : '' }
function courseTypeLabel(c) { return c.courseType === 'private' ? '私教' : '团课' }
function isOver(c) {
  if (c.status && c.status !== 'open') return true
  if (c.endTime) { const end = new Date(c.endTime); if (!isNaN(end) && end.getTime() < Date.now()) return true }
  return false
}
function seatText(c) {
  if (isOver(c)) return '已结束'
  const r = Math.max(0, (c.capacity || 0) - (c.bookedCount || 0))
  return r === 0 ? '已约满' : '剩' + r + '席'
}
function openCourse(c) { uni.navigateTo({ url: '/pages/booking/detail?id=' + c.id }) }

// ==================== 生成课表图片（Canvas 绘制） ====================
const imgVisible = ref(false)
const imgPath = ref('')
const generating = ref(false)

// 画布几何常量（750 设计宽）
const CW = 750, MARGIN = 20, MONTH_W = 44, GAP = 8
const DAY_W = 87, HEAD_H = 64
const CARD_H = 190, CARD_HEAD_H = 80
// Blackpink 配色
const C_PINK = '#ff2d9b', C_PINK_L = '#ff6fb5', C_BG = '#111114', C_CELL = '#1a1a1d', C_CARD = '#1c1c20'
const C_WHITE = '#ffffff', C_GRAY = '#9a9aa2', C_LGRAY = '#c9c9d1'

function generateImage() {
  if (generating.value) return
  generating.value = true
  uni.showLoading({ title: '正在生成…', mask: true })
  renderWeekCanvas().then((path) => {
    imgPath.value = path
    imgVisible.value = true
  }).catch(() => {
    uni.showToast({ title: '生成失败，请重试', icon: 'none' })
  }).finally(() => {
    generating.value = false
    uni.hideLoading()
  })
}

/** 计算画布布局：返回总高与每行像素信息 */
function buildCanvasLayout() {
  const headX = MARGIN + MONTH_W + GAP
  // 每个时段行高 = 单元格内最多课程数决定
  const rows = weekSegments.value.map(seg => {
    const maxCount = Math.max(0, ...seg.cells.map(c => c.length))
    const h = maxCount === 0 ? 140 : maxCount * (CARD_H + GAP) - GAP + 16
    return { seg, h }
  })
  const gridTop = 268
  let y = gridTop + HEAD_H + 14
  rows.forEach(r => { r.y = y; y += r.h + 14 })
  const height = y + 60   // 底部品牌署名
  return { rows, headX, gridTop, height }
}

async function renderWeekCanvas() {
  const node = await new Promise((resolve) => {
    uni.createSelectorQuery().select('#svCanvas').fields({ node: true, size: true }).exec(res => resolve(res && res[0] && res[0].node))
  })
  if (!node) return Promise.reject(new Error('canvas 未就绪'))

  const layout = buildCanvasLayout()
  const dpr = Math.min(2, uni.getSystemInfoSync().pixelRatio || 2)
  node.width = CW * dpr
  node.height = Math.ceil(layout.height) * dpr
  const ctx = node.getContext('2d')
  ctx.scale(dpr, dpr)
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  // 背景
  ctx.fillStyle = C_BG
  ctx.fillRect(0, 0, CW, layout.height)

  // 顶部：logo 圆 + 标题 + 日期范围
  await drawBrandHeader(ctx, node)

  // 表头：月份 + 7 天
  drawDayHeader(ctx, layout)

  // 预下载头像（去重，失败用首字占位）
  const avatarMap = await preloadAvatars(node)

  // 时段行
  layout.rows.forEach(row => {
    drawSegmentRow(ctx, row, avatarMap, layout.headX)
  })

  // 底部署名
  ctx.fillStyle = C_GRAY
  ctx.font = '18px sans-serif'
  ctx.fillText('玩舞尚志 · DANCE CASUALLY · 让热爱自由发生', CW / 2, layout.height - 34)

  // 导出图片
  return new Promise((resolve, reject) => {
    uni.canvasToTempFilePath({
      canvas: node,
      fileType: 'png',
      success: r => resolve(r.tempFilePath),
      fail: reject
    })
  })
}

/** 顶部品牌区：白色圆 + logo + 标题 */
async function drawBrandHeader(ctx, node) {
  const cx = CW / 2, cy = 92, r = 62
  ctx.fillStyle = C_WHITE
  ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.fill()
  try {
    const logo = await loadImage(node, '/static/logo.png')
    const s = Math.min(r * 1.5 / logo.width, r * 1.5 / logo.height)
    const w = logo.width * s, h = logo.height * s
    ctx.drawImage(logo, cx - w / 2, cy - h / 2, w, h)
  } catch (e) { /* logo 加载失败则仅保留白色圆 */ }
  ctx.fillStyle = C_WHITE
  ctx.font = 'bold 32px sans-serif'
  ctx.fillText('玩舞尚志 · 本周课表', cx, cy + r + 32)
  ctx.fillStyle = C_GRAY
  ctx.font = '18px sans-serif'
  ctx.fillText('DANCE CASUALLY · 让热爱自由发生', cx, cy + r + 60)
  const first = weekDays.value[0], last = weekDays.value[6]
  if (first && last) {
    ctx.fillStyle = C_PINK_L
    ctx.font = 'bold 26px sans-serif'
    ctx.fillText(first.date.substring(5).replace('-', '.') + ' - ' + last.date.substring(5).replace('-', '.'), cx, cy + r + 92)
  }
}

/** 表头：左侧月份 + 7 天粉色块 */
function drawDayHeader(ctx, layout) {
  const y = layout.gridTop
  ctx.fillStyle = C_WHITE
  ctx.font = 'bold 24px sans-serif'
  ctx.fillText(weekMonth.value + '月', MARGIN + MONTH_W / 2, y + HEAD_H / 2)
  weekDays.value.forEach((d, i) => {
    const x = layout.headX + i * (DAY_W + GAP)
    roundRect(ctx, x, y, DAY_W, HEAD_H, 12)
    const g = ctx.createLinearGradient(x, y, x + DAY_W, y + HEAD_H)
    g.addColorStop(0, C_PINK); g.addColorStop(1, C_PINK_L)
    ctx.fillStyle = g; ctx.fill()
    ctx.fillStyle = C_WHITE
    ctx.font = 'bold 20px sans-serif'
    ctx.fillText('周' + d.cn, x + DAY_W / 2, y + 20)
    ctx.font = 'bold 24px sans-serif'
    ctx.fillText(d.day + '日', x + DAY_W / 2, y + 45)
  })
}

/** 一个时段行：左侧竖排时段名 + 7 列单元格 */
function drawSegmentRow(ctx, row, avatarMap, headX) {
  const { seg, y, h } = row
  // 左侧时段条
  roundRect(ctx, MARGIN, y, MONTH_W, h, 22)
  const g = ctx.createLinearGradient(MARGIN, y, MARGIN, y + h)
  g.addColorStop(0, C_PINK); g.addColorStop(1, C_PINK_L)
  ctx.fillStyle = g; ctx.fill()
  ctx.fillStyle = C_WHITE
  ctx.font = 'bold 24px sans-serif'
  const chars = seg.label.split('')
  const startY = y + h / 2 - (chars.length - 1) * 17
  chars.forEach((ch, i) => ctx.fillText(ch, MARGIN + MONTH_W / 2, startY + i * 34))

  // 7 列单元格 + 课程卡
  seg.cells.forEach((courses, di) => {
    const x = headX + di * (DAY_W + GAP)
    roundRect(ctx, x, y, DAY_W, h, 12)
    ctx.fillStyle = C_CELL; ctx.fill()
    courses.forEach((c, i) => {
      drawCourseCard(ctx, c, x + 8, y + 8 + i * (CARD_H + GAP), DAY_W - 16, avatarMap)
    })
  })
}

/** 课程卡片：粉色头部(头像+老师名) + 深色信息区 + 席位角标 */
function drawCourseCard(ctx, c, x, y, w, avatarMap) {
  const cx = x + w / 2
  // 卡片底 + 分区（裁剪圆角后分两段填色）
  ctx.save()
  roundRect(ctx, x, y, w, CARD_H, 14)
  ctx.clip()
  const g = ctx.createLinearGradient(x, y, x + w, y + CARD_HEAD_H)
  g.addColorStop(0, C_PINK); g.addColorStop(1, C_PINK_L)
  ctx.fillStyle = g; ctx.fillRect(x, y, w, CARD_HEAD_H)
  ctx.fillStyle = C_CARD; ctx.fillRect(x, y + CARD_HEAD_H, w, CARD_H - CARD_HEAD_H)
  ctx.restore()

  // 头像
  drawAvatar(ctx, avatarMap[c.teacherAvatar] || null, cx, y + 30, 22, c.teacherName)
  // 老师名
  ctx.fillStyle = C_WHITE
  ctx.font = 'bold 19px sans-serif'
  ctx.fillText(fitText(ctx, c.teacherName || '老师', w - 8), cx, y + 64)
  // 信息区
  ctx.fillText(fitText(ctx, c.danceTypeName || c.courseName || '', w - 8), cx, y + CARD_HEAD_H + 16)
  ctx.fillStyle = C_LGRAY
  ctx.font = '15px sans-serif'
  ctx.fillText(fmtHM(c.startTime) + '-' + fmtHM(c.endTime), cx, y + CARD_HEAD_H + 38)
  ctx.fillStyle = C_GRAY
  ctx.font = '15px sans-serif'
  ctx.fillText(fitText(ctx, courseTypeLabel(c) + '｜' + (c.levelName || '常规'), w - 6), cx, y + CARD_HEAD_H + 58)
  // 席位角标
  const over = isOver(c), txt = seatText(c)
  ctx.font = 'bold 15px sans-serif'
  const bw = ctx.measureText(txt).width + 20
  roundRect(ctx, cx - bw / 2, y + CARD_H - 32, bw, 24, 8)
  ctx.fillStyle = over ? '#3a3a40' : C_PINK; ctx.fill()
  ctx.fillStyle = over ? '#b9b9c1' : C_WHITE
  ctx.fillText(txt, cx, y + CARD_H - 20)
}

/** 圆形头像（cover 裁剪），无图时画首字占位 */
function drawAvatar(ctx, img, cx, cy, r, name) {
  ctx.save()
  ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.clip()
  if (img) {
    const s = Math.max(2 * r / img.width, 2 * r / img.height)
    const dw = img.width * s, dh = img.height * s
    ctx.drawImage(img, cx - dw / 2, cy - dh / 2, dw, dh)
  } else {
    ctx.fillStyle = '#3a3a40'
    ctx.fillRect(cx - r, cy - r, 2 * r, 2 * r)
    ctx.fillStyle = C_WHITE
    ctx.font = 'bold 20px sans-serif'
    ctx.fillText((name || 'T')[0], cx, cy + 1)
  }
  ctx.restore()
  ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(255,255,255,.85)'; ctx.lineWidth = 2; ctx.stroke()
}

async function preloadAvatars(node) {
  const map = {}
  const urls = [...new Set(weekCourses.value.map(c => c.teacherAvatar).filter(Boolean))]
  await Promise.all(urls.map(u => loadImage(node, fileUrl(u)).then(img => { map[u] = img }).catch(() => {})))
  return map
}

function loadImage(node, src) {
  return new Promise((resolve, reject) => {
    const img = node.createImage()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

/** 文本超宽截断加省略号（需先设置 ctx.font） */
function fitText(ctx, text, maxW) {
  if (ctx.measureText(text).width <= maxW) return text
  let t = text
  while (t.length > 1 && ctx.measureText(t + '…').width > maxW) t = t.slice(0, -1)
  return t + '…'
}

/** 圆角矩形路径 */
function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

/** 保存课表图片到相册 */
function saveImage() {
  if (!imgPath.value) return
  uni.saveImageToPhotosAlbum({
    filePath: imgPath.value,
    success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
    fail: (e) => {
      const msg = (e && e.errMsg) || ''
      if (msg.indexOf('auth') >= 0 || msg.indexOf('denied') >= 0) {
        uni.showModal({
          title: '提示', content: '需要相册权限才能保存，请在设置中开启',
          confirmText: '去设置',
          success: (r) => { if (r.confirm) uni.openSetting() }
        })
      } else {
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    }
  })
}

buildDays()
loadWeek()
</script>

<style lang="scss" scoped>
.sv-page { min-height: 100vh; background: #0e0e0e; }
.sv-toolbar { padding: 20rpx 30rpx 6rpx; display: flex; align-items: center; justify-content: space-between; }
.sv-tabs { display: flex; gap: 14rpx; }
.sv-tab { height: 62rpx; padding: 0 30rpx; display: flex; align-items: center; border: 1rpx solid #2e2e33; border-radius: 32rpx; background: #1a1a1d; color: #9a9aa2; font-size: 25rpx; font-weight: 700; }
/* Blackpink 风：黑底 + 荧光粉胶囊 */
.sv-tab.on { border-color: #ff2d9b; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); color: #fff; font-weight: 900; box-shadow: 0 6rpx 20rpx rgba(255,45,155,.38); }
.sv-right { display: flex; align-items: center; gap: 22rpx; }
.sv-filter { color: #ff5fae; font-size: 24rpx; font-weight: 800; }
.sv-curdate { color: #fff; font-size: 24rpx; font-weight: 800; }
.sv-date-scroll { width: 100%; border-bottom: 1rpx solid #2e2e33; }
.sv-dates { display: inline-flex; }
.sv-date-item { position: relative; width: 118rpx; flex: none; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 18rpx 0 24rpx; color: #9a9aa2; }
.sv-date-item text { font-size: 21rpx; font-weight: 700; letter-spacing: 1rpx; }
.sv-date-item view { margin-top: 12rpx; color: #9a9aa2; font-size: 30rpx; font-weight: 800; }
.sv-date-item.active text, .sv-date-item.active view { color: #ff5fae; }
.sv-date-item.active::after { content: ''; position: absolute; bottom: 8rpx; width: 44rpx; height: 6rpx; border-radius: 4rpx; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); }
.sv-body { padding: 24rpx 30rpx; }
.sv-empty { padding: 90rpx 20rpx; border: 1rpx solid #2e2e33; border-radius: 24rpx; background: #17171a; color: #777780; font-size: 24rpx; text-align: center; }
.wv-pad { margin: 24rpx 30rpx; }

/* ===== 周视图 ===== */
.wv-actions { display: flex; justify-content: flex-end; padding: 16rpx 30rpx 0; }
.wv-gen { padding: 12rpx 28rpx; border: 1rpx solid rgba(255,45,155,.45); border-radius: 30rpx; background: rgba(255,45,155,.08); color: #ff5fae; font-size: 25rpx; font-weight: 800; letter-spacing: 2rpx; }
.wv-wrap { padding: 0 20rpx 40rpx; }
.wv-headrow { display: flex; gap: 8rpx; margin-top: 18rpx; }
.wv-month { width: 60rpx; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 25rpx; font-weight: 900; }
.wv-dayhead { min-width: 0; flex: 1; min-height: 88rpx; border-radius: 14rpx; background: linear-gradient(135deg,#ff2d9b,#ff6fb5); box-shadow: 0 6rpx 16rpx rgba(255,45,155,.26); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4rpx; }
.wv-dayhead.today { box-shadow: 0 6rpx 22rpx rgba(255,45,155,.6); }
.wv-dayhead text { font-size: 20rpx; font-weight: 800; }
.wv-dayhead view { font-size: 24rpx; font-weight: 900; }
.wv-segrow { display: flex; gap: 8rpx; margin-top: 14rpx; align-items: stretch; }
.wv-seglabel { width: 60rpx; border-radius: 30rpx; background: linear-gradient(180deg,#ff2d9b,#ff6fb5); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; font-size: 24rpx; font-weight: 900; padding: 20rpx 0; }
.wv-cells { min-width: 0; flex: 1; display: flex; gap: 8rpx; }
.wv-cell { min-width: 0; flex: 1; border-radius: 12rpx; background: #1a1a1d; padding: 8rpx; display: flex; flex-direction: column; gap: 8rpx; }
.wv-card { border-radius: 14rpx; overflow: hidden; }
.wv-card-top { background: linear-gradient(135deg,#ff2d9b,#ff6fb5); padding: 12rpx 6rpx 10rpx; display: flex; flex-direction: column; align-items: center; gap: 6rpx; }
.wv-avatar { width: 56rpx; height: 56rpx; border-radius: 50%; border: 2rpx solid rgba(255,255,255,.85); background: #fff; }
.wv-avatar.ph { display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22rpx; font-weight: 900; background: rgba(255,255,255,.25); }
.wv-tname { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #fff; font-size: 20rpx; font-weight: 800; }
.wv-card-body { background: #1c1c20; padding: 10rpx 6rpx 12rpx; display: flex; flex-direction: column; align-items: center; gap: 4rpx; }
.wv-dance { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #fff; font-size: 20rpx; font-weight: 800; }
.wv-time { color: #c9c9d1; font-size: 18rpx; }
.wv-meta { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #9a9aa2; font-size: 17rpx; }
.wv-badge { margin-top: 4rpx; padding: 2rpx 12rpx; border-radius: 10rpx; background: #ff2d9b; color: #fff; font-size: 17rpx; font-weight: 800; }
.wv-badge.over { background: #3a3a40; color: #b9b9c1; font-weight: 600; }

/* ===== 课表图片预览弹层 ===== */
.img-mask { position: fixed; inset: 0; z-index: 999; background: #101014; display: flex; flex-direction: column; }
.img-close { position: absolute; right: 36rpx; top: calc(30rpx + env(safe-area-inset-top)); width: 64rpx; height: 64rpx; border-radius: 50%; background: #26262b; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 32rpx; z-index: 2; }
.img-scroll { min-height: 0; flex: 1; }
.img-pic-wrap { padding: 30rpx 40rpx; }
.img-pic { display: block; width: 100%; border-radius: 16rpx; }
.img-save { margin: 20rpx 40rpx calc(30rpx + env(safe-area-inset-bottom)); height: 92rpx; border-radius: 46rpx; background: linear-gradient(90deg,#ff2d9b,#ff6fb5); box-shadow: 0 10rpx 30rpx rgba(255,45,155,.4); color: #fff; font-size: 30rpx; font-weight: 900; letter-spacing: 6rpx; display: flex; align-items: center; justify-content: center; }

/* 离屏画布（不可见） */
.sv-canvas { position: fixed; left: -9999px; top: 0; width: 375px; height: 600px; }
</style>
