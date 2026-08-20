import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：
// - server.port = 5173
// - dev server 代理 /api 到后端 8080，避免浏览器跨域
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
