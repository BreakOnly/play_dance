<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">签到管理</span>
      <div style="display:flex;gap:8px">
        <el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始" end-placeholder="结束" @change="load" />
        <el-button @click="loadStat">出勤统计</el-button>
        <el-button type="primary" @click="codeVisible = true">展示签到码</el-button>
        <el-button type="primary" @click="openManual">管理员代签</el-button>
      </div>
    </div>

    <el-row :gutter="12" style="margin-bottom:12px" v-if="stat">
      <el-col :span="8"><el-statistic title="签到次数" :value="stat.checkinCount || 0" /></el-col>
      <el-col :span="8"><el-statistic title="约课数" :value="stat.bookingCount || 0" /></el-col>
      <el-col :span="8"><el-statistic title="爽约数" :value="stat.noshowCount || 0" /></el-col>
    </el-row>

    <el-table :data="list" border>
      <el-table-column prop="userName" label="用户" width="140" />
      <el-table-column prop="courseName" label="课程" />
      <el-table-column label="方式" width="90"><template #default="{row}">{{ row.method==='manual'?'代签':'扫码' }}</template></el-table-column>
      <el-table-column prop="checkTime" label="签到时间" width="180" />
    </el-table>

    <el-dialog v-model="codeVisible" title="微信扫一扫签到" width="480px" @closed="code = null">
      <el-select v-model="codeSchedule" placeholder="选择正在签到的课次" style="width:100%" @change="code = null">
        <el-option v-for="s in schedules" :key="s.id" :label="optLabel(s)" :value="s.id" />
      </el-select>
      <div v-if="code" style="text-align:center;margin-top:20px">
        <img :src="code.image" alt="本课次微信签到码" style="width:300px;max-width:100%" />
        <p>有效期至 {{ code.expiresAt }}，仅限本人扫码签到</p>
      </div>
      <p v-else>请在开课前允许签到的时段内生成，签到码有效期为 5 分钟。</p>
      <template #footer><el-button type="primary" :loading="codeLoading" :disabled="!codeSchedule" @click="generateCode">生成 / 刷新签到码</el-button></template>
    </el-dialog>
    <el-dialog v-model="manualVisible" title="管理员代签" width="440px">
      <el-form label-width="80px">
        <el-form-item label="课次">
          <el-select v-model="mSchedule" filterable placeholder="选择课次" style="width:100%" @change="loadBooked">
            <el-option v-for="s in schedules" :key="s.id" :label="optLabel(s)" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户">
          <el-select v-model="mBooking" placeholder="选择待上课用户" style="width:100%">
            <el-option v-for="b in bookedList" :key="b.id" :label="b.userName" :value="b.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="manualVisible=false">取消</el-button><el-button type="primary" @click="doManual">确认代签</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { checkinApi, scheduleApi, bookingApi } from '@/api'

const list = ref([]); const range = ref([]); const stat = ref(null)
const schedules = ref([])
const codeVisible = ref(false), codeSchedule = ref(null), code = ref(null), codeLoading = ref(false)
async function generateCode() {
  if (codeLoading.value) return
  codeLoading.value = true
  try { code.value = await checkinApi.code(codeSchedule.value) } finally { codeLoading.value = false }
}
const manualVisible = ref(false); const mSchedule = ref(null); const mBooking = ref(null); const bookedList = ref([])

function load() {
  const params = {}
  if (range.value && range.value.length === 2) { params.from = range.value[0]; params.to = range.value[1] }
  checkinApi.records(params).then((d) => list.value = d || [])
}
function loadStat() {
  const params = {}
  if (range.value && range.value.length === 2) { params.from = range.value[0]; params.to = range.value[1] }
  checkinApi.attendance(params).then((d) => stat.value = d || {})
}
function openManual() { mSchedule.value = null; mBooking.value = null; bookedList.value = []; manualVisible.value = true }
function loadBooked() {
  mBooking.value = null
  bookingApi.bySchedule(mSchedule.value).then((d) => bookedList.value = (d || []).filter((b) => b.status === 'booked'))
}
function doManual() {
  if (!mBooking.value) return
  checkinApi.manual({ bookingId: mBooking.value }).then(() => { ElMessage.success('代签成功'); manualVisible.value = false; load() })
}
function optLabel(s) { return `${(s.startTime||'').substring(5,16).replace('T',' ')} ${s.courseName}` }

onMounted(() => {
  scheduleApi.list({}).then((d) => schedules.value = d || [])
  load()
})
</script>
