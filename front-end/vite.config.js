import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
const apiProxy = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      // 浏览器请求 /api 代理到后端，规避跨域
      '/api': {
        target: apiProxy,
        changeOrigin: true
      },
      '/files': {
        target: apiProxy,
        changeOrigin: true
      }
    }
  }
})
