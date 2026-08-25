<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">排课管理</span>
      <div style="display:flex;gap:8px">
        <el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss"
          start-placeholder="开始" end-placeholder="结束" @change="load" />
        <el-button type="primary" @click="open()">新增排课</el-button>
      </div>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="courseName" label="课程" />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="room" label="教室" width="90" />
      <el-table-column label="时间" min-width="220">
        <template #default="{row}">{{ fmt(row.startTime) }} ~ {{ row.endTime ? row.endTime.substring(11,16) : '' }}</template>
      </el-table-column>
      <el-table-column label="约课" width="100"><template #default="{row}">{{ row.bookedCount }}/{{ row.capacity }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{row}"><el-tag>{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button link type="danger" :disabled="row.status==='cancelled'" @click="cancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新增排课" width="460px">
      <el-form :model="f" label-width="80px">
        <el-form-item label="课程"><el-select v-model="f.courseId" filterable><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="教师"><el-select v-model="f.teacherId" clearable><el-option v-for="t in teachers" :key="t.id" :label="t.name" :value="t.id" /></el-select></el-form-item>
        <el-form-item label="教室"><el-input v-model="f.room" /></el-form-item>
        <el-form-item label="开始"><el-date-picker v-model="f.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="结束"><el-date-picker v-model="f.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="f.capacity" :min="1" /><span style="color:#909399;margin-left:8px">默认12</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleApi, courseApi, teacherApi } from '@/api'

const list = ref([]); const courses = ref([]); const teachers = ref([])
const range = ref([])
const visible = ref(false); const f = ref({})

function load() {
  const params = {}
  if (range.value && range.value.length === 2) { params.start = range.value[0]; params.end = range.value[1] }
  scheduleApi.list(params).then((d) => list.value = d || [])
}
function open() { f.value = { capacity: undefined }; visible.value = true }
function save() {
  scheduleApi.save(f.value).then(() => { ElMessage.success('排课成功'); visible.value = false; load() })
}
function cancel(row) { ElMessageBox.confirm('取消该课次？').then(() => scheduleApi.cancel(row.id).then(() => { ElMessage.success('已取消'); load() })).catch(() => {}) }
function statusText(s) { return { open: '报名中', full: '满员', finished: '已结束', cancelled: '已取消' }[s] || s }
function fmt(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' }

onMounted(() => {
  courseApi.courses().then((d) => courses.value = d || [])
  teacherApi.list().then((d) => teachers.value = d || [])
  load()
})
</script>
