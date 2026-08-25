<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">手办管理</span>
      <div style="display:flex;gap:8px">
        <el-radio-group v-model="cat" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="claw">抓取款</el-radio-button>
          <el-radio-button value="redeem">兑换款</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="open()">新增手办</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="name" label="名称" />
      <el-table-column label="类别" width="100"><template #default="{row}"><el-tag :type="row.category==='claw'?'warning':'success'">{{ row.category==='claw'?'抓取款':'兑换款' }}</el-tag></template></el-table-column>
      <el-table-column prop="pointsRequired" label="所需积分" width="100" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'">{{ row.status===1?'上架':'下架' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{row}">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="f.id?'编辑手办':'新增手办'" width="480px">
      <el-form :model="f" label-width="90px">
        <el-form-item label="名称"><el-input v-model="f.name" /></el-form-item>
        <el-form-item label="类别">
          <el-radio-group v-model="f.category"><el-radio value="claw">抓取款</el-radio><el-radio value="redeem">兑换款</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="所需积分"><el-input-number v-model="f.pointsRequired" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="f.stock" :min="0" /></el-form-item>
        <el-form-item label="多角度图">
          <el-upload :action="uploadImgUrl" :headers="uploadHeaders" :show-file-list="false" accept="image/*" :on-success="onImgUploaded">
            <el-button>上传图片</el-button>
          </el-upload>
          <div class="img-preview" v-if="previews.length">
            <div v-for="(im,i) in previews" :key="i" class="img-item">
              <el-image :src="im" fit="cover" style="width:64px;height:64px;border-radius:6px" />
              <span class="del-img" @click="removeImg(i)">×</span>
            </div>
          </div>
          <el-input v-model="imagesText" type="textarea" placeholder="也可直接粘贴图片URL，多张用逗号分隔" style="margin-top:8px" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="f.description" type="textarea" /></el-form-item>
        <el-form-item label="上架"><el-switch v-model="f.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { goodsApi } from '@/api'

const list = ref([]); const cat = ref(''); const visible = ref(false); const f = ref({}); const imagesText = ref('')
const uploadImgUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: 'Bearer ' + (localStorage.getItem('admin_token') || '') }
const previews = computed(() => imagesText.value ? imagesText.value.split(/[,，]/).map((s) => s.trim()).filter(Boolean) : [])
function onImgUploaded(res) {
  if (res && res.code === 0) {
    imagesText.value = previews.value.concat(res.data.url).join(',')
    ElMessage.success('上传成功')
  } else { ElMessage.error('上传失败') }
}
function removeImg(i) { const arr = previews.value.slice(); arr.splice(i, 1); imagesText.value = arr.join(',') }

function load() { goodsApi.list(cat.value ? { category: cat.value } : {}).then((d) => list.value = d || []) }
function open(row) {
  if (row) {
    f.value = { ...row }
    try { imagesText.value = (row.images ? JSON.parse(row.images) : []).join(',') } catch (e) { imagesText.value = '' }
  } else {
    f.value = { category: 'redeem', pointsRequired: 100, stock: 1, status: 1 }
    imagesText.value = ''
  }
  visible.value = true
}
function save() {
  const payload = { ...f.value, images: imagesText.value ? imagesText.value.split(/[,，]/).map((s) => s.trim()).filter(Boolean) : [] }
  goodsApi.save(payload).then(() => { ElMessage.success('已保存'); visible.value = false; load() })
}
function del(row) { ElMessageBox.confirm('删除该手办？').then(() => goodsApi.del(row.id).then(() => { ElMessage.success('已删除'); load() })).catch(() => {}) }
onMounted(load)
</script>

<style scoped>
.img-preview { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px; }
.img-item { position: relative; }
.del-img { position: absolute; top: -8px; right: -8px; width: 18px; height: 18px; line-height: 16px; text-align: center; background: #f56c6c; color: #fff; border-radius: 50%; cursor: pointer; font-size: 14px; }
</style>
