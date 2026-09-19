import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  // 必须保留：删掉后连 .vue 都无法编译（实测 "Build failed with errors"）
  // 当前锁定的 @dcloudio 版本默认导出是 CJS 模块对象，因此要用 uni.default()
  plugins: [uni.default()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})