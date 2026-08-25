<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">发放管理</span>
      <div style="display:flex;gap:8px">
        <el-select v-model="status" placeholder="状态" clearable style="width:130px" @change="load">
          <el-option label="待审批" value="pending" /><el-option label="待发货" value="to_ship" />
          <el-option label="配送中" value="shipping" /><el-option label="已完成" value="completed" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-select v-model="source" placeholder="来源" clearable style="width:120px" @change="load">
          <el-option label="抓取" value="claw" /><el-option label="兑换" value="redeem" />
        </el-select>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="单号" width="70" />
      <el-table-column prop="goodsName" label="手办" />
      <el-table-column label="来源" width="80"><template #default="{row}">{{ row.source==='claw'?'抓取':'兑换' }}</template></el-table-column>
      <el-table-column prop="pointsCost" label="积分" width="80" />
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="tagType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="收货" min-width="180">
        <template #default="{row}"><span v-if="row.receiverName">{{ row.receiverName }} {{ row.receiverPhone }} {{ row.address }}</span><span v-else style="color:#c0c4cc">未填</span></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{row}">
          <template v-if="row.status==='pending'">
            <el-button link type="primary" @click="approve(row)">通过</el-button>
            <el-button link type="danger" @click="reject(row)">驳回</el-button>
          </template>
          <el-button link type="primary" v-if="row.status==='to_ship'" @click="ship(row)">填物流</el-button>
          <el-button link type="success" v-if="row.status==='shipping'" @click="complete(row)">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { redeemApi } from '@/api'

const list = ref([]); const status = ref(''); const source = ref('')
function load() {
  const p = {}; if (status.value) p.status = status.value; if (source.value) p.source = source.value
  redeemApi.list(p).then((d) => list.value = d || [])
}
function approve(row) { redeemApi.approve(row.id).then(() => { ElMessage.success('已通过'); load() }) }
function reject(row) {
  ElMessageBox.prompt('驳回原因', '驳回', { inputPlaceholder: '可填写原因' })
    .then(({ value }) => redeemApi.reject(row.id, value || '').then(() => { ElMessage.success('已驳回，积分已退'); load() })).catch(() => {})
}
function ship(row) {
  ElMessageBox.prompt('物流公司,单号（逗号分隔）', '填写物流', { inputPlaceholder: 'SF,SF123456' })
    .then(({ value }) => {
      const p = (value || '').split(/[,，]/)
      redeemApi.logistics(row.id, { company: p[0] || '', no: p[1] || '' }).then(() => { ElMessage.success('已发货'); load() })
    }).catch(() => {})
}
function complete(row) { redeemApi.complete(row.id).then(() => { ElMessage.success('已完成'); load() }) }
function statusText(s) { return { pending: '待审批', to_ship: '待发货', shipping: '配送中', completed: '已完成', rejected: '已驳回' }[s] || s }
function tagType(s) { return { pending: 'warning', to_ship: 'primary', shipping: '', completed: 'success', rejected: 'info' }[s] || 'info' }
onMounted(load)
</script>
