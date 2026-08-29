import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'path'
// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  plugins: [
    uni(),
  ],
  // H5 开发时监听所有网卡，便于手机通过局域网 IP 访问预览
  server: {
    host: '0.0.0.0',
    // H5 同源代理：页面相对路径请求转发到本机后端
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/files': 'http://127.0.0.1:8080'
    }
  },
})
