<template>
  <div class="app-container">
    <slot></slot>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './store/user'

const router = useRouter()
const userStore = useUserStore()

// 初始化用户信息
onMounted(() => {
  userStore.initUserInfo()
  
  // 路由守卫
  router.beforeEach((to, from, next) => {
    const isLoggedIn = userStore.getIsLoggedIn
    const publicPages = ['/pages/login/login', '/pages/register/register']
    const isPublicPage = publicPages.includes(to.path)
    
    if (!isLoggedIn && !isPublicPage) {
      // 未登录且访问非公共页面，重定向到登录页
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