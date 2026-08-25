<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">Banner 管理</span>
      <div style="display:flex;gap:8px">
        <el-button type="primary" @click="openEdit()">新增 Banner</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column label="图片" width="140">
        <template #default="{ row }">
          <el-image :src="row.imageUrl" style="width:120px;height:60px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column prop="subtitle" label="副标题" min-width="160" />
      <el-table-column label="跳转类型" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.linkType === 'none'" type="info">无跳转</el-tag>
          <el-tag v-else-if="row.linkType === 'tab'" type="success">内部页面</el-tag>
          <el-tag v-else type="warning">外部链接</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="linkValue" label="跳转目标" min-width="180" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status===1?'success':'info'">{{ row.status===1?'上架':'下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑 -->
    <el-dialog v-model="editVisible" :title="form.id?'编辑 Banner':'新增 Banner'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="图片">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="onUploadSuccess"
            accept="image/*"
          >
            <el-image v-if="form.imageUrl" :src="form.imageUrl" style="width:200px;height:100px;border:1px solid var(--el-border-color)" fit="cover" />
            <el-button v-else>点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subtitle" /></el-form-item>
        <el-form-item label="跳转类型">
          <el-radio-group v-model="form.linkType">
            <el-radio value="none">无跳转</el-radio>
            <el-radio value="tab">内部页面</el-radio>
            <el-radio value="url">外部链接</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="跳转目标">
          <el-input v-model="form.linkValue" :disabled="form.linkType === 'none'" placeholder="内部页面路径或外部URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bannerApi, uploadUrl } from '@/api'

const list = ref([]); const loading = ref(false)

const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + (localStorage.getItem('admin_token') || '')
}))

function load() {
  loading.value = true
  bannerApi.list().then((d) => { list.value = d || [] }).finally(() => loading.value = false)
}

const editVisible = ref(false)
const form = ref({})
function openEdit(row) {
  form.value = row ? { ...row } : { imageUrl: '', title: '', subtitle: '', linkType: 'none', linkValue: '', sort: 0, status: 1 }
  editVisible.value = true
}
function onUploadSuccess(res) {
  if (res && res.code === 0 && res.data) {
    form.value.imageUrl = res.data
  } else if (res && res.url) {
    form.value.imageUrl = res.url
  } else if (typeof res === 'string') {
    form.value.imageUrl = res
  } else {
    ElMessage.error('上传失败')
  }
}
function save() {
  bannerApi.save(form.value).then(() => {
    ElMessage.success('已保存')
    editVisible.value = false
    load()
  })
}
function del(row) {
  ElMessageBox.confirm('确定要删除该 Banner 吗？', '提示', { type: 'warning' }).then(() => {
    bannerApi.del(row.id).then(() => { ElMessage.success('已删除'); load() })
  }).catch(() => {})
}

onMounted(() => { load() })
</script>
