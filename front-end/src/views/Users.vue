<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">用户管理</span>
      <div style="display:flex;gap:8px">
        <el-input v-model="q.keyword" placeholder="姓名/手机" clearable style="width:200px" @keyup.enter="reload" />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column label="姓名" width="110"><template #default="{ row }">{{ row.name || '未命名' }}</template></el-table-column>
      <el-table-column label="手机" width="140"><template #default="{ row }">{{ row.phone || '—' }}</template></el-table-column>
      <el-table-column prop="courseBalance" label="可用课时" width="90" />
      <el-table-column prop="pointsBalance" label="积分" width="90" />
      <el-table-column label="操作" min-width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">明细</el-button>
          <el-button link type="primary" @click="openRecharge(row)">充值</el-button>
          <el-button link type="primary" @click="openPoints(row)">积分</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total,prev,pager,next" :total="total" :page-size="q.size" :current-page="q.page" @current-change="onPage" />

    <!-- 新增/编辑 -->
    <el-dialog v-model="editVisible" title="编辑用户资料" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="微信手机号"><el-input v-model="form.phone" disabled /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender"><el-radio :value="1">男</el-radio><el-radio :value="2">女</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" @click="saveUser">保存</el-button></template>
    </el-dialog>

    <!-- 充值 -->
    <el-dialog v-model="rechargeVisible" title="课时充值" width="420px">
      <el-form :model="rechargeForm" label-width="90px">
        <el-form-item label="充值课时"><el-input-number v-model="rechargeForm.hours" :min="1" /></el-form-item>
        <el-form-item label="赠送课时"><el-input-number v-model="rechargeForm.giftHours" :min="0" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="rechargeForm.amount" :min="0" /></el-form-item>
        <el-form-item label="有效期(天)"><el-input-number v-model="rechargeForm.validityDays" :min="0" /><span style="color:#909399;margin-left:8px">0=永久</span></el-form-item>
        <el-form-item label="备注"><el-input v-model="rechargeForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="rechargeVisible=false">取消</el-button><el-button type="primary" @click="doRecharge">确认充值</el-button></template>
    </el-dialog>

    <!-- 积分 -->
    <el-dialog v-model="pointsVisible" title="积分增减" width="420px">
      <el-form :model="pointsForm" label-width="80px">
        <el-form-item label="变动值"><el-input-number v-model="pointsForm.delta" /><span style="color:#909399;margin-left:8px">正加负减</span></el-form-item>
        <el-form-item label="原因"><el-input v-model="pointsForm.reason" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="pointsVisible=false">取消</el-button><el-button type="primary" @click="doPoints">提交</el-button></template>
    </el-dialog>

    <!-- 明细 -->
    <el-drawer v-model="detailVisible" :title="cur.name + ' · 明细'" size="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="可用课时">{{ cur.courseBalance }}</el-descriptions-item>
        <el-descriptions-item label="积分">{{ cur.pointsBalance }}</el-descriptions-item>
      </el-descriptions>
      <el-tabs v-model="logTab" style="margin-top:12px">
        <el-tab-pane label="课时流水" name="hour">
          <el-table :data="hourLogs" size="small" max-height="380">
            <el-table-column prop="type" label="类型" width="90" />
            <el-table-column prop="amount" label="变动" width="70" />
            <el-table-column prop="balanceAfter" label="余" width="60" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column prop="createdAt" label="时间" width="150" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="积分流水" name="point">
          <el-table :data="pointLogs" size="small" max-height="380">
            <el-table-column prop="type" label="类型" width="90" />
            <el-table-column prop="amount" label="变动" width="70" />
            <el-table-column prop="balanceAfter" label="余" width="70" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column prop="createdAt" label="时间" width="150" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'

const list = ref([]); const total = ref(0); const loading = ref(false)
const q = ref({ keyword: '', page: 1, size: 10 })
const saving = ref(false)
const rechargeKey = ref(''), pointsKey = ref('')
function operationKey() { return Date.now().toString(36) + Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2) }
async function mutate(action, close) {
  if (saving.value) return
  saving.value = true
  try { await action(); ElMessage.success('已保存'); close(); load() } finally { saving.value = false }
}

function reload() { q.value.page = 1; load() }
function onPage(p) { q.value.page = p; load() }
function load() {
  loading.value = true
  userApi.list({ ...q.value }).then((d) => { list.value = d.list || []; total.value = d.total || 0 }).finally(() => loading.value = false)
}

// 编辑
const editVisible = ref(false)
const form = ref({})
function openEdit(row) { form.value = { ...row }; editVisible.value = true }
function saveUser() {
  mutate(() => userApi.update(form.value.id, { name: form.value.name, gender: form.value.gender }), () => { editVisible.value = false })
}

// 充值
const rechargeVisible = ref(false); const rechargeForm = ref({})
function openRecharge(row) { rechargeKey.value = operationKey(); rechargeForm.value = { id: row.id, hours: 10, giftHours: 0, amount: 0, validityDays: 365, remark: '' }; rechargeVisible.value = true }
function doRecharge() {
  mutate(() => userApi.recharge(rechargeForm.value.id, rechargeForm.value, rechargeKey.value), () => { rechargeVisible.value = false })
}

// 积分
const pointsVisible = ref(false); const pointsForm = ref({})
function openPoints(row) { pointsKey.value = operationKey(); pointsForm.value = { id: row.id, delta: 0, reason: '' }; pointsVisible.value = true }
function doPoints() {
  mutate(() => userApi.adjustPoints(pointsForm.value.id, { delta: pointsForm.value.delta, reason: pointsForm.value.reason }, pointsKey.value), () => { pointsVisible.value = false })
}

// 明细
const detailVisible = ref(false); const cur = ref({}); const logTab = ref('hour')
const hourLogs = ref([]); const pointLogs = ref([])
function openDetail(row) {
  cur.value = row; detailVisible.value = true
  userApi.hourLogs(row.id).then((d) => hourLogs.value = d || [])
  userApi.pointLogs(row.id).then((d) => pointLogs.value = d || [])
}

onMounted(() => { load() })
</script>
