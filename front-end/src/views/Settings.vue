<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">系统设置 · 业务规则</span>
      <el-button type="primary" @click="save">保存全部</el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="remark" label="设置项" min-width="220" />
      <el-table-column label="值" width="200">
        <template #default="{row}"><el-input v-model="row.configValue" /></template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" />
    </el-table>
    <div class="tip">新预约使用保存后的规则；已有预约按原课时和签到积分结算。候补不冻结课时，转正时冻结。商品到店领取。</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi } from '@/api'

const list = ref([])
function load() { configApi.list().then((d) => list.value = d || []) }
function save() {
  const map = {}
  list.value.forEach((c) => { map[c.configKey] = String(c.configValue) })
  configApi.update(map).then(() => { ElMessage.success('已保存'); load() })
}
onMounted(load)
</script>

<style scoped>
.tip { color: #909399; font-size: 12px; margin-top: 12px; }
</style>
