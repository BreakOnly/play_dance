<template>
  <div class="login">
    <div class="box">
      <div class="logo">M</div>
      <div class="title">街舞工作室 · 管理后台</div>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <div class="hint">默认账号 admin / admin123</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'

const router = useRouter()
const form = ref({ username: 'admin', password: 'admin123' })
const loading = ref(false)

function submit() {
  loading.value = true
  authApi.login(form.value).then((d) => {
    localStorage.setItem('admin_token', d.token)
    ElMessage.success('登录成功')
    router.push('/students')
  }).catch(() => {}).finally(() => { loading.value = false })
}
</script>

<style scoped>
.login { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #2a0f1f, #0E0E0E); }
.box { width: 380px; background: #1a1a1d; border: 1px solid #2e2e33; border-radius: 16px; padding: 40px 36px; box-shadow: 0 20px 60px rgba(255,45,155,.25); }
.logo { width: 60px; height: 60px; border-radius: 14px; margin: 0 auto 16px; background: linear-gradient(135deg, #FF2D9B, #FF6FB5); color: #fff; font-size: 32px; font-weight: 900; display: flex; align-items: center; justify-content: center; }
.title { text-align: center; font-size: 20px; font-weight: 800; margin-bottom: 26px; color: #fff; }
.hint { text-align: center; color: #6b6b73; font-size: 12px; margin-top: 10px; }
</style>
