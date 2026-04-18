/**
 * MomentKeep 朝暮记 - 应用入口文件
 * @description 初始化Vue应用实例，配置Pinia状态管理
 * @author MomentKeep Team
 * @since 2026-04-18
 */

import { createApp } from 'vue'
import App from './App.vue'
import pinia from './store'

/**
 * 创建Vue应用实例
 * @description 使用createApp创建根组件实例，并注册Pinia状态管理插件
 */
const app = createApp(App)

// 注册Pinia状态管理插件
app.use(pinia)

// 挂载应用到#app元素
app.mount('#app')