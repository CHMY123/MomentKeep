/**
 * MomentKeep 朝暮记 - 应用入口文件
 * @description 初始化Vue应用实例，配置Pinia状态管理
 * @author MomentKeep Team
 * @since 2026-04-18
 */

import { createSSRApp } from 'vue'
import App from './App.vue'
import pinia from './store'

/**
 * uni-app 三端统一入口。
 *
 * 必须导出 createApp 工厂函数（H5 / 微信小程序 / App 均由各自平台运行时调用），
 * 不能像纯 Vue 项目那样直接 createApp().mount('#app')，
 * 否则小程序端无法启动、H5 端也会丢失 uni 生命周期。
 */
export function createApp() {
  const app = createSSRApp(App)
  app.use(pinia)
  return { app }
}
