<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <span class="logo">M</span>
        <!-- 门店名取自后台门店信息，兜底默认文案 -->
        <span class="bname">{{ admin.storeName || '街舞工作室' }}</span>
      </div>
      <el-menu :default-active="$route.path" router :collapse="false" background-color="transparent" text-color="#cfd3dc" active-text-color="#FF6FB5">
        <el-menu-item v-for="r in menus" :key="r.path" :index="'/' + r.path">
          <el-icon><component :is="r.meta.icon" /></el-icon>
          <span>{{ r.name }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="crumb">{{ $route.name }}</div>
        <el-dropdown @command="onCmd">
          <span class="user">{{ admin.nickname || admin.username || '管理员' }} <el-icon><ArrowDown /></el-icon></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
    <el-dialog v-model="passwordVisible" title="修改管理员密码" width="420px">
      <el-form label-width="85px">
        <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password placeholder="至少 12 个字符" /></el-form-item>
      </el-form>
      <template #footer><el-button :loading="saving" type="primary" @click="savePassword">修改并重新登录</el-button></template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'

const router = useRouter()
const admin = ref({})
const passwordVisible = ref(false), password = ref({ oldPassword: '', newPassword: '' }), saving = ref(false)
async function savePassword() {
  if (saving.value) return
  saving.value = true
  try { await authApi.password(password.value); localStorage.removeItem('admin_token'); router.replace('/login') } finally { saving.value = false }
}
const menus = router.getRoutes().find((r) => r.path === '/').children.filter((c) => c.name)

onMounted(() => {
  authApi.info().then((d) => { admin.value = d }).catch(() => {})
})

async function onCmd(cmd) {
  if (cmd === 'password') { password.value = { oldPassword: '', newPassword: '' }; passwordVisible.value = true; return }
  if (cmd === 'logout') {
    await authApi.logout()
    localStorage.removeItem('admin_token')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #0a0a0b; color: #fff; border-right: 1px solid #2e2e33; }
.brand { display: flex; align-items: center; gap: 10px; padding: 18px 20px; color: #fff; border-bottom: 1px solid #2e2e33; }
.logo { width: 34px; height: 34px; border-radius: 8px; background: linear-gradient(135deg, #FF2D9B, #FF6FB5); display: flex; align-items: center; justify-content: center; font-weight: 900; }
.bname { font-weight: 700; font-size: 16px; }
.aside :deep(.el-menu) { border-right: none; }
.header { background: #1a1a1d; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #2e2e33; }
.crumb { font-weight: 600; color: #fff; }
.user { cursor: pointer; color: #9a9aa2; display: flex; align-items: center; gap: 4px; }
.main { background: #0E0E0E; padding: 16px; }
</style>
