<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">视频管理</span>
      <el-button type="primary" @click="open()">新增视频</el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="createdAt" label="发布时间" width="170" />
      <el-table-column prop="likeCount" label="点赞数" width="90" />
      <el-table-column prop="fileUrl" label="地址" show-overflow-tooltip />
      <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="del(row)">删除</el-button></template></el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新增视频" width="480px">
      <el-form :model="f" label-width="90px">
        <el-form-item label="标题"><el-input v-model="f.title" /></el-form-item>
        <el-form-item label="视频文件">
          <el-upload
            ref="uploadRef"
            :action="uploadVideoUrl"
            :headers="headers"
            :auto-upload="false"
            :limit="1"
            accept="video/*"
            :on-exceed="onExceed"
            :on-change="syncPending"
            :on-remove="syncPending"
            :on-success="onUploaded"
            :on-error="onUploadError"
          >
            <el-button>选择视频</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ saving ? '上传中…' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox, genFileId } from 'element-plus'
import { videoApi } from '@/api'

const list = ref([]); const visible = ref(false); const f = ref({})
const uploadRef = ref(); const saving = ref(false); const hasPending = ref(false)
const uploadVideoUrl = '/api/admin/upload/video'
const headers = { Authorization: 'Bearer ' + (localStorage.getItem('admin_token') || '') }

function load() { videoApi.publicList().then((d) => list.value = d || []) }
function open() {
  f.value = {}; saving.value = false; hasPending.value = false; visible.value = true
  nextTick(() => uploadRef.value && uploadRef.value.clearFiles())
}
// 同步"已选择、尚未上传"状态（auto-upload=false，选择文件不触发上传）
function syncPending(_file, fileList) { hasPending.value = fileList.some((x) => x.status === 'ready') }
// 已选过文件再重选：直接替换旧选择；旧文件从未上传，不会在 OSS 留残留
function onExceed(files) {
  uploadRef.value.clearFiles()
  const file = files[0]
  file.uid = genFileId()
  uploadRef.value.handleStart(file)
}
function onUploaded(res) {
  if (res && res.code === 0) { f.value.fileUrl = res.data.url; publish() } else { onUploadError() }
}
function onUploadError() {
  saving.value = false; hasPending.value = false
  uploadRef.value && uploadRef.value.clearFiles()
  ElMessage.error('视频上传失败，请重新选择文件')
}
function save() {
  if (!f.value.title) { ElMessage.warning('请填写标题'); return }
  if (hasPending.value) { saving.value = true; uploadRef.value.submit(); return } // 上传完成后在 onUploaded 里自动发布
  if (!f.value.fileUrl) { ElMessage.warning('请先选择视频文件'); return }
  saving.value = true; publish()
}
function publish() {
  videoApi.publish(f.value).then(() => {
    ElMessage.success('已发布'); visible.value = false; load()
  }).catch(() => {}).finally(() => { saving.value = false })
}
function del(row) { ElMessageBox.confirm('下架该视频？').then(() => videoApi.hidePublic(row.id).then(() => { ElMessage.success('已下架'); load() })).catch(() => {}) }
onMounted(load)
</script>
