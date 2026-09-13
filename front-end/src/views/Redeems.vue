<template>
  <div class="page-card">
    <div class="page-toolbar"><span class="page-title">积分商品 · 到店领取</span>
      <el-select v-model="status" clearable placeholder="全部状态" style="width:150px" @change="load"><el-option v-for="(label,value) in states" :key="value" :label="label" :value="value" /></el-select>
    </div>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="单号" width="80" />
      <el-table-column prop="userName" label="学员" /><el-table-column prop="userPhone" label="微信手机号" />
      <el-table-column prop="goodsName" label="商品" /><el-table-column prop="pointsCost" label="积分" width="90" />
      <el-table-column label="状态"><template #default="{row}">{{ states[row.status] || row.status }}</template></el-table-column>
      <el-table-column prop="pickedUpAt" label="领取时间" />
      <el-table-column label="操作" width="220"><template #default="{row}">
        <template v-if="row.status==='pending'"><el-button link type="primary" :disabled="busy" @click="approve(row)">通过</el-button><el-button link type="danger" :disabled="busy" @click="reject(row)">驳回</el-button></template>
        <el-button v-if="row.status==='ready'" link type="primary" :disabled="busy" @click="pickup(row)">核对领取码并发放</el-button>
      </template></el-table-column>
    </el-table>
    <p>请核对本人姓名和兑换记录，再输入学员出示的领取码。已领取订单不能重复核销。</p>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { redeemApi } from '@/api'
const states = { pending: '待审批', ready: '待领取', completed: '已领取', rejected: '已驳回' }
const list = ref([]), status = ref(''), busy = ref(false), loading = ref(false)
async function load() { loading.value = true; try { list.value = await redeemApi.list({ status: status.value }) || [] } finally { loading.value = false } }
async function mutate(fn) { if (busy.value) return; busy.value = true; try { await fn(); ElMessage.success('已处理'); await load() } finally { busy.value = false } }
function approve(row) { mutate(() => redeemApi.approve(row.id)) }
async function reject(row) {
  try { const { value } = await ElMessageBox.prompt('请填写驳回原因，积分和库存将退回', '驳回兑换', { inputValidator: v => !!v?.trim() || '原因不能为空' }); await mutate(() => redeemApi.reject(row.id, value)) } catch {}
}
async function pickup(row) {
  try { const { value } = await ElMessageBox.prompt(`确认向 ${row.userName || '该学员'} 发放 ${row.goodsName}，请输入学员出示的领取码`, '到店核销', { inputValidator: v => /^[a-zA-Z0-9]{12}$/.test(v?.trim() || '') || '请输入 12 位领取码' }); await mutate(() => redeemApi.complete(row.id, value.trim())) } catch {}
}
onMounted(load)
</script>
