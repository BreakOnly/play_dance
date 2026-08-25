<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">教师管理</span>
      <el-button type="primary" @click="open()">新增教师</el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column prop="intro" label="简介" />
      <el-table-column label="状态" width="80">
        <template #default="{row}"><el-tag :type="row.status===1?'success':'info'">{{ row.status===1?'正常':'停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{row}">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="f.id?'编辑教师':'新增教师'" width="440px">
      <el-form :model="f" label-width="90px">
        <el-form-item label="姓名"><el-input v-model="f.name" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="f.phone" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="f.intro" type="textarea" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="f.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherApi } from '@/api'

const list = ref([]); const visible = ref(false); const f = ref({})
function load() { teacherApi.list().then((d) => list.value = d || []) }
function open(row) { f.value = row ? { ...row } : { status: 1 }; visible.value = true }
function save() {
  teacherApi.save(f.value).then(() => { ElMessage.success('已保存'); visible.value = false; load() })
}
function del(row) { ElMessageBox.confirm('删除该教师？').then(() => teacherApi.del(row.id).then(() => { ElMessage.success('已删除'); load() })).catch(() => {}) }
onMounted(load)
</script>
