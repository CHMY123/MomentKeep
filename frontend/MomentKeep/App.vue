<template>
  <!-- 应用容器 - 根组件 -->
  <div class="app-container">
    <slot></slot>
  </div>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 根组件
 * @description 应用根组件，负责用户状态初始化
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { onMounted } from 'vue'
import { useUserStore } from './store/user'

const userStore = useUserStore()

/**
 * 初始化用户信息
 * @description 组件挂载时从本地存储恢复用户登录状态
 */
onMounted(() => {
  console.log('App组件挂载，开始初始化用户信息')
  userStore.initUserInfo()
  console.log('用户信息初始化完成，登录状态:', userStore.getIsLoggedIn)

  // 微信小程序环境不需要Vue Router的路由守卫
  // 登录状态检查在各个页面单独处理
})
</script>

<style>
/* 全局样式 */
:root {
  --bg-color: #F8F6F2;
  --text-color: #333333;
  --primary-color: #C2977F;
  --secondary-color: #94A7C8;
}

page, view, text, image, navigator, button, input, textarea, picker, picker-view {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: var(--bg-color);
  color: var(--text-color);
  transition: all 0.3s ease;
}

.app-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-color);
  color: var(--text-color);
  transition: all 0.3s ease;
}
</style>