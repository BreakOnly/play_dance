<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">约课管理</span>
      <div style="display:flex;gap:8px">
        <el-select v-model="scheduleId" placeholder="全部未结束课次" filterable clearable style="width:360px" @change="load">
          <el-option v-for="s in schedules" :key="s.id" :label="optLabel(s)" :value="s.id" />
        </el-select>
        <el-button type="primary" :disabled="!scheduleId" @click="openBook">代约用户</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column v-if="!scheduleId" label="课次" min-width="220">
        <template #default="{row}">{{ fmtDt(row.startTime) }} {{ row.courseName }}</template>
      </el-table-column>
      <el-table-column prop="userName" label="用户" width="140" />
      <el-table-column label="状态" width="120"><template #default="{row}"><el-tag :type="tagType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column prop="waitlistNo" label="候补序号" width="100" />
      <el-table-column label="约课时间" min-width="160"><template #default="{row}">{{ fmtDt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button link type="danger" v-if="row.status==='booked'||row.status==='waitlist'" @click="cancel(row)">代取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!list.length" description="暂无约课记录" />

    <el-dialog v-model="bookVisible" title="代约用户" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-select v-model="bookUserId" filterable placeholder="选择用户" style="width:100%">
            <el-option v-for="s in users" :key="s.id" :label="`${s.name||'(未命名)'} ${s.phone||''}`" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="bookVisible=false">取消</el-button><el-button type="primary" @click="doBook">确认代约</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookingApi, scheduleApi, userApi } from '@/api'

const schedules = ref([]); const scheduleId = ref(null); const list = ref([])
const users = ref([])
const bookVisible = ref(false); const bookUserId = ref(null)

function load() {
  bookingApi.bySchedule(scheduleId.value || null).then((d) => list.value = d || [])
}
function openBook() { bookUserId.value = null; bookVisible.value = true }
function doBook() {
  if (!bookUserId.value) return
  bookingApi.book({ userId: bookUserId.value, scheduleId: scheduleId.value })
    .then(() => { ElMessage.success('代约成功'); bookVisible.value = false; load() })
}
function cancel(row) { ElMessageBox.confirm('代取消该约课？').then(() => bookingApi.cancel(row.id).then(() => { ElMessage.success('已取消'); load() })).catch(() => {}) }

function optLabel(s) { return `${(s.startTime||'').substring(5,16).replace('T',' ')} ${s.courseName} (${s.bookedCount}/${s.capacity})` }
function fmtDt(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' }
function statusText(s) { return { booked: '待上课', checked: '已签到', waitlist: '候补中', cancelled: '已取消', noshow: '爽约' }[s] || s }
function tagType(s) { return { booked: 'primary', checked: 'success', waitlist: 'warning', cancelled: 'info', noshow: 'danger' }[s] || 'info' }

onMounted(() => {
  load()
  const n = new Date()
  const p = (v) => String(v).padStart(2, '0')
  const start = `${n.getFullYear()}-${p(n.getMonth() + 1)}-${p(n.getDate())}T${p(n.getHours())}:${p(n.getMinutes())}:${p(n.getSeconds())}`
  scheduleApi.list({ start }).then((d) => schedules.value = (d || []).filter((s) => s.status !== 'cancelled'))
  userApi.list({ page: 1, size: 200 }).then((d) => users.value = d.list || [])
})
</script>
