<template>
  <!-- 应用容器 - 根组件 -->
  <div class="app-container">
    <slot></slot>
  </div>
</template>

<script setup>
/**
 * MomentKeep 朝暮记 - 根组件
 * @description 应用根组件，负责全局路由守卫和用户状态初始化
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './store/user'

const router = useRouter()
const userStore = useUserStore()

/**
 * 初始化用户信息
 * @description 组件挂载时从本地存储恢复用户登录状态
 */
onMounted(() => {
  userStore.initUserInfo()

  /**
   * 路由守卫
   * @description 验证用户登录状态，未登录用户访问非公共页面时重定向到登录页
   */
  router.beforeEach((to, from, next) => {
    const isLoggedIn = userStore.getIsLoggedIn
    const publicPages = ['/pages/login/login', '/pages/register/register']
    const isPublicPage = publicPages.includes(to.path)

    if (!isLoggedIn && !isPublicPage) {
      next('/pages/login/login')
    } else {
      next()
    }
  })
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

* {
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