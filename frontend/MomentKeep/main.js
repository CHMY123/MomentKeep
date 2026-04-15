import { createApp } from 'vue'
import App from './App.vue'
import pinia from './store'

// 创建应用实例
const app = createApp(App)

// 使用插件
app.use(pinia)

// 挂载应用
app.mount('#app')