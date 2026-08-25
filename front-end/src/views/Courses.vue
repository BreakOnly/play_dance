<template>
  <div class="page-card">
    <el-tabs v-model="tab">
      <!-- 课程 -->
      <el-tab-pane label="课程" name="course">
        <div class="page-toolbar">
          <span></span>
          <el-button type="primary" @click="openCourse()">新增课程</el-button>
        </div>
        <el-table :data="courses" border>
          <el-table-column prop="name" label="课程名" />
          <el-table-column label="类型" width="90"><template #default="{row}">{{ row.type==='private'?'私教':'团课' }}</template></el-table-column>
          <el-table-column prop="danceTypeName" label="舞种" width="100" />
          <el-table-column prop="levelName" label="难度" width="90" />
          <el-table-column prop="durationMin" label="时长" width="80" />
          <el-table-column prop="price" label="价格" width="90" />
          <el-table-column prop="costHours" label="耗课时" width="80" />
          <el-table-column label="操作" width="140">
            <template #default="{row}">
              <el-button link type="primary" @click="openCourse(row)">编辑</el-button>
              <el-button link type="danger" @click="delCourse(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 舞种 -->
      <el-tab-pane label="舞种" name="dance">
        <div class="dict-add">
          <el-input v-model="newDance" placeholder="新增舞种名称" style="width:220px" />
          <el-button type="primary" @click="addDance">新增</el-button>
        </div>
        <el-table :data="danceTypes" border style="margin-top:12px">
          <el-table-column label="名称"><template #default="{row}"><el-input v-model="row.name" @blur="saveDance(row)" /></template></el-table-column>
          <el-table-column label="排序" width="180"><template #default="{row}"><el-input-number v-model="row.sort" :min="0" @change="saveDance(row)" /></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="delDance(row.id)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 星级 -->
      <el-tab-pane label="星级" name="level">
        <div class="dict-add">
          <el-input v-model="newLevel" placeholder="新增星级名称（如：一星入门）" style="width:260px" />
          <el-button type="primary" @click="addLevel" :disabled="levels.length >= 5">新增</el-button>
          <span v-if="levels.length >= 5" style="color:#909399;font-size:12px;margin-left:8px">最多5个星级</span>
        </div>
        <el-table :data="levels" border style="margin-top:12px">
          <el-table-column label="名称"><template #default="{row}"><el-input v-model="row.name" @blur="saveLevel(row)" /></template></el-table-column>
          <el-table-column label="星级" width="180"><template #default="{row}"><el-input-number v-model="row.sort" :min="1" :max="5" @change="saveLevel(row)" /></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="delLevel(row.id)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="courseVisible" :title="cf.id?'编辑课程':'新增课程'" width="480px">
      <el-form :model="cf" label-width="80px">
        <el-form-item label="课程名"><el-input v-model="cf.name" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="cf.type"><el-radio value="group">团课</el-radio><el-radio value="private">私教</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="舞种"><el-select v-model="cf.danceTypeId"><el-option v-for="d in danceTypes" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="难度"><el-select v-model="cf.levelId"><el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id" /></el-select></el-form-item>
        <el-form-item label="时长(分)"><el-input-number v-model="cf.durationMin" :min="15" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="cf.price" :min="0" /></el-form-item>
        <el-form-item label="耗课时"><el-input-number v-model="cf.costHours" :min="1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="courseVisible=false">取消</el-button><el-button type="primary" @click="saveCourse">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi } from '@/api'

const tab = ref('course')
const courses = ref([]); const danceTypes = ref([]); const levels = ref([])
const newDance = ref(''); const newLevel = ref('')

function loadAll() {
  courseApi.danceTypes().then((d) => danceTypes.value = d || [])
  courseApi.levels().then((d) => levels.value = d || [])
  courseApi.courses().then((d) => courses.value = d || [])
}

const courseVisible = ref(false); const cf = ref({})
function openCourse(row) { cf.value = row ? { ...row } : { type: 'group', durationMin: 60, price: 0, costHours: 1 }; courseVisible.value = true }
function saveCourse() { courseApi.saveCourse(cf.value).then(() => { ElMessage.success('已保存'); courseVisible.value = false; loadAll() }) }
function delCourse(row) { ElMessageBox.confirm('删除该课程？').then(() => courseApi.delCourse(row.id).then(() => { ElMessage.success('已删除'); loadAll() })).catch(() => {}) }

function addDance() { if (!newDance.value) return; courseApi.saveDanceType({ name: newDance.value, sort: 0, status: 1 }).then(() => { newDance.value = ''; loadAll() }) }
function saveDance(row) { courseApi.saveDanceType(row).then(() => {}) }
function delDance(id) { courseApi.delDanceType(id).then(() => { ElMessage.success('已删除'); loadAll() }) }
function addLevel() { if (!newLevel.value) return; courseApi.saveLevel({ name: newLevel.value, sort: 1, status: 1 }).then(() => { newLevel.value = ''; loadAll() }) }
function saveLevel(row) { courseApi.saveLevel(row).then(() => {}) }
function delLevel(id) { courseApi.delLevel(id).then(() => { ElMessage.success('已删除'); loadAll() }) }

onMounted(loadAll)
</script>

<style scoped>
.dict-add { display: flex; gap: 8px; }
</style>
