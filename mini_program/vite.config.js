import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'path'
const apiProxy = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080'
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
  // 默认仅监听本机；真机联调时显式开放，生产不运行开发服务器。
  server: {
    host: '127.0.0.1',
    // H5 同源代理：页面相对路径请求转发到本机后端
    proxy: {
      '/api': apiProxy,
      '/files': apiProxy
    }
  },
})
