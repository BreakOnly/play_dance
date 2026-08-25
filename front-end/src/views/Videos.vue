<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">视频管理</span>
      <el-button type="primary" @click="open()">新增视频</el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="videoDate" label="日期" width="120" />
      <el-table-column label="可见范围" width="140"><template #default="{row}">{{ scopeText(row.scopeType) }}</template></el-table-column>
      <el-table-column prop="fileUrl" label="地址" show-overflow-tooltip />
      <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="del(row)">删除</el-button></template></el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新增视频" width="480px">
      <el-form :model="f" label-width="90px">
        <el-form-item label="标题"><el-input v-model="f.title" /></el-form-item>
        <el-form-item label="所属课程"><el-select v-model="f.courseId" clearable><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="所属课次"><el-input-number v-model="f.scheduleId" :min="1" placeholder="课次ID" /></el-form-item>
        <el-form-item label="课堂日期"><el-date-picker v-model="f.videoDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="f.scopeType">
            <el-option label="该课已签到学员" value="checked_in" />
            <el-option label="该课已约课学员" value="booked" />
          </el-select>
        </el-form-item>
        <el-form-item label="视频文件">
          <el-upload :action="uploadVideoUrl" :headers="headers" :show-file-list="true" :limit="1" :on-success="onUploaded">
            <el-button>上传视频</el-button>
          </el-upload>
          <el-input v-model="f.fileUrl" placeholder="或直接填入视频URL" style="margin-top:8px" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { videoApi, courseApi } from '@/api'

const list = ref([]); const courses = ref([]); const visible = ref(false); const f = ref({})
const uploadVideoUrl = '/api/admin/upload/video'
const headers = { Authorization: 'Bearer ' + (localStorage.getItem('admin_token') || '') }

function load() { videoApi.list({}).then((d) => list.value = d || []) }
function open() { f.value = { scopeType: 'checked_in' }; visible.value = true }
function onUploaded(res) { if (res && res.code === 0) { f.value.fileUrl = res.data.url; ElMessage.success('上传成功') } else { ElMessage.error('上传失败') } }
function save() {
  if (!f.value.fileUrl) { ElMessage.warning('请先上传或填写视频地址'); return }
  videoApi.create(f.value).then(() => { ElMessage.success('已保存'); visible.value = false; load() })
}
function del(row) { ElMessageBox.confirm('删除该视频？').then(() => videoApi.del(row.id).then(() => { ElMessage.success('已删除'); load() })).catch(() => {}) }
function scopeText(s) { return { checked_in: '已签到学员', booked: '已约课学员', custom: '指定学员' }[s] || s }
onMounted(() => { courseApi.courses().then((d) => courses.value = d || []); load() })
</script>
