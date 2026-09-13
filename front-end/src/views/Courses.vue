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
          <el-table-column prop="tag" label="标签" width="110"><template #default="{row}">{{ row.tag || '—' }}</template></el-table-column>
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

      <!-- 教室 -->
      <el-tab-pane label="教室" name="room">
        <div class="dict-add">
          <el-input v-model="newRoom" placeholder="新增教室名称（如：A教室）" style="width:260px" />
          <el-button type="primary" @click="addRoom">新增</el-button>
        </div>
        <el-table :data="rooms" border style="margin-top:12px">
          <el-table-column label="名称"><template #default="{row}"><el-input v-model="row.name" @blur="saveRoom(row)" /></template></el-table-column>
          <el-table-column label="最大容纳人数(0不限)" width="200"><template #default="{row}"><el-input-number v-model="row.maxCapacity" :min="0" @change="saveRoom(row)" /></template></el-table-column>
          <el-table-column label="排序" width="180"><template #default="{row}"><el-input-number v-model="row.sort" :min="0" @change="saveRoom(row)" /></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="delRoom(row.id)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 注意事项 -->
      <el-tab-pane label="注意事项" name="notice">
        <div class="page-toolbar">
          <span style="color:#909399;font-size:12px">排课时可选用；小程序课程详情页展示所选内容</span>
          <el-button type="primary" @click="openNotice()">新增注意事项</el-button>
        </div>
        <el-table :data="notices" border>
          <el-table-column prop="name" label="名称" width="180" />
          <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="操作" width="140">
            <template #default="{row}">
              <el-button link type="primary" @click="openNotice(row)">编辑</el-button>
              <el-button link type="danger" @click="delNotice(row)">删除</el-button>
            </template>
          </el-table-column>
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
        <el-form-item label="标签"><el-input v-model="cf.tag" maxlength="30" placeholder="选填，小程序课程卡片角标，如：零基础友好" clearable /></el-form-item>
        <el-form-item label="时长(分)"><el-input-number v-model="cf.durationMin" :min="15" /></el-form-item>
        <el-form-item label="价格(元)">
          <el-input v-model="cf.price" placeholder="请输入价格" @input="onPriceInput">
            <template #suffix>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="耗课时"><el-input-number v-model="cf.costHours" :min="1" /></el-form-item>
        <el-form-item label="课程简介"><el-input v-model="cf.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="选填，小程序课程详情页展示" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="courseVisible=false">取消</el-button><el-button type="primary" @click="saveCourse">保存</el-button></template>
    </el-dialog>

    <!-- 注意事项编辑弹窗 -->
    <el-dialog v-model="noticeVisible" :title="nf.id?'编辑注意事项':'新增注意事项'" width="520px">
      <el-form :model="nf" label-width="80px">
        <el-form-item label="名称"><el-input v-model="nf.name" maxlength="50" placeholder="如：常规课注意事项" /></el-form-item>
        <el-form-item label="内容">
          <el-input v-model="nf.content" type="textarea" :rows="8" maxlength="2000" show-word-limit
                    placeholder="支持换行；小程序课程详情页「注意事项」板块展示" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="nf.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="noticeVisible=false">取消</el-button><el-button type="primary" @click="saveNotice">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, noticeApi } from '@/api'

const tab = ref('course')
const courses = ref([]); const danceTypes = ref([]); const levels = ref([]); const rooms = ref([]); const notices = ref([])
const newDance = ref(''); const newLevel = ref(''); const newRoom = ref('')

function refreshDanceTypes() { return courseApi.danceTypes().then((d) => danceTypes.value = d || []) }
function refreshLevels() { return courseApi.levels().then((d) => levels.value = d || []) }
function refreshRooms() { return courseApi.rooms().then((d) => rooms.value = d || []) }
function loadAll() {
  refreshDanceTypes()
  refreshLevels()
  refreshRooms()
  courseApi.courses().then((d) => courses.value = d || [])
  noticeApi.list().then((d) => notices.value = d || [])
}

const courseVisible = ref(false); const cf = ref({})
function openCourse(row) { cf.value = row ? { ...row } : { type: 'group', durationMin: 60, price: 0, costHours: 1 }; courseVisible.value = true }
function onPriceInput(v) {
  let s = String(v).replace(/[^\d.]/g, '')
  const dot = s.indexOf('.')
  if (dot !== -1) s = s.slice(0, dot + 1) + s.slice(dot + 1).replace(/\./g, '')
  cf.value.price = s
}
function saveCourse() {
  const data = { ...cf.value, price: Number(cf.value.price) || 0 }
  courseApi.saveCourse(data).then(() => { ElMessage.success('已保存'); courseVisible.value = false; loadAll() })
}
function delCourse(row) { ElMessageBox.confirm('删除该课程？').then(() => courseApi.delCourse(row.id).then(() => { ElMessage.success('已删除'); loadAll() })).catch(() => {}) }

// 新增舞种默认排在已有舞种之后
function addDance() {
  if (!newDance.value) return
  const maxSort = danceTypes.value.reduce((m, d) => Math.max(m, d.sort), 0)
  courseApi.saveDanceType({ name: newDance.value, sort: maxSort + 1, status: 1 })
    .then(() => { newDance.value = ''; refreshDanceTypes() })
}
// 保存后重新拉取，按 sort 升序即时重排；失败时回滚为库里的值
function saveDance(row) { courseApi.saveDanceType(row).then(refreshDanceTypes).catch(refreshDanceTypes) }
function delDance(id) { courseApi.delDanceType(id).then(() => { ElMessage.success('已删除'); loadAll() }) }
// 新增星级默认取 1~5 中未被占用的最小星级
function addLevel() {
  if (!newLevel.value) return
  const used = new Set(levels.value.map(l => l.sort))
  let sort = 1
  for (let i = 1; i <= 5; i++) { if (!used.has(i)) { sort = i; break } }
  courseApi.saveLevel({ name: newLevel.value, sort, status: 1 })
    .then(() => { newLevel.value = ''; refreshLevels() })
}
// 保存后重新拉取，按星级升序即时重排；星级冲突等校验失败时提示并回滚
function saveLevel(row) { courseApi.saveLevel(row).then(refreshLevels).catch(refreshLevels) }
function delLevel(id) { courseApi.delLevel(id).then(() => { ElMessage.success('已删除'); loadAll() }) }
// 新增教室默认排在已有教室之后
function addRoom() {
  if (!newRoom.value) return
  const maxSort = rooms.value.reduce((m, r) => Math.max(m, r.sort), 0)
  courseApi.saveRoom({ name: newRoom.value, sort: maxSort + 1, status: 1 })
    .then(() => { newRoom.value = ''; refreshRooms() })
}
// 保存后重新拉取，按 sort 升序即时重排；失败时回滚为库里的值
function saveRoom(row) { courseApi.saveRoom(row).then(refreshRooms).catch(refreshRooms) }
function delRoom(id) { courseApi.delRoom(id).then(() => { ElMessage.success('已删除'); refreshRooms() }) }

// 注意事项（排课时选填关联，小程序课程详情展示）
const noticeVisible = ref(false); const nf = ref({})
function openNotice(row) { nf.value = row ? { ...row } : { sort: 0, status: 1 }; noticeVisible.value = true }
function saveNotice() {
  noticeApi.save(nf.value).then(() => { ElMessage.success('已保存'); noticeVisible.value = false; noticeApi.list().then((d) => notices.value = d || []) })
}
function delNotice(row) { ElMessageBox.confirm('删除该注意事项？被排课引用时将无法删除。').then(() => noticeApi.del(row.id).then(() => { ElMessage.success('已删除'); noticeApi.list().then((d) => notices.value = d || []) })).catch(() => {}) }

onMounted(loadAll)
</script>

<style scoped>
.dict-add { display: flex; gap: 8px; }
</style>
