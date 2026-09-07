<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="m in metrics" :key="m.label">
        <div class="metric page-card">
          <div class="m-label">{{ m.label }}</div>
          <div class="m-value">{{ m.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">课时不足用户（{{ lowHours.length }}）</div>
          <el-table :data="lowHours" size="small" max-height="320" empty-text="暂无">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="phone" label="手机" />
            <el-table-column prop="courseBalance" label="剩余课时" width="90" />
            <el-table-column prop="pointsBalance" label="积分" width="80" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">可能流失（14天未到课，{{ inactive.length }}）</div>
          <el-table :data="inactive" size="small" max-height="320" empty-text="暂无">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="phone" label="手机" />
            <el-table-column prop="lastCheckin" label="最近到课" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { dashboardApi } from '@/api'

const overview = ref({})
const lowHours = ref([])
const inactive = ref([])

const metrics = computed(() => [
  { label: '累计营收(元)', value: overview.value.revenue ?? 0 },
  { label: '充值课时', value: overview.value.rechargeHours ?? 0 },
  { label: '签到次数', value: overview.value.checkinCount ?? 0 },
  { label: '活跃/用户', value: `${overview.value.activeUsers ?? 0}/${overview.value.userTotal ?? 0}` }
])

onMounted(() => {
  dashboardApi.overview().then((d) => { overview.value = d || {} })
  dashboardApi.alerts().then((d) => { lowHours.value = d.lowHours || []; inactive.value = d.inactive || [] })
})
</script>

<style scoped>
.metric { text-align: center; padding: 22px 0; }
.m-label { color: #909399; font-size: 13px; }
.m-value { font-size: 30px; font-weight: 800; color: #FF2D9B; margin-top: 8px; }
</style>
