<template>
  <view class="login-container">
    <view class="login-card">
      <!-- LOGO 区域（已修复） -->
      <view class="logo-container">
        <image src="../../static/logo.png" class="logo" mode="aspectFit"></image>
        <view class="title-wrapper">
          <text class="app-name">朝暮记</text>
          <text class="app-slogan">朝有目标，暮有记录</text>
        </view>
      </view>

      <!-- 用户名 -->
      <view class="form-item">
        <text class="label">用户名</text>
        <input
          v-model="formData.username"
          type="text"
          placeholder="请输入用户名"
          class="input-box"
        />
      </view>

      <!-- 密码 -->
      <view class="form-item">
        <text class="label">密码</text>
        <input
          v-model="formData.password"
          type="password"
          placeholder="请输入密码"
          class="input-box"
        />
      </view>

      <!-- 记住我 -->
      <view class="checkbox-item">
        <checkbox v-model="formData.remember" />
        <text class="checkbox-text">记住我</text>
      </view>

      <!-- 协议 -->
      <view class="checkbox-item">
        <checkbox v-model="formData.agreement" />
        <text class="checkbox-text">我已阅读并同意用户协议和隐私政策</text>
      </view>

      <!-- 登录按钮 -->
      <button class="login-btn" @click="handleLogin" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <!-- 注册链接 -->
      <view class="register-row">
        <text>还没有账号？</text>
        <text class="link" @click="goToRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

const formData = ref({
  username: '',
  password: '',
  remember: false,
  agreement: false
})

const loading = ref(false)

// 登录
const handleLogin = async () => {
  if (!formData.value.username || !formData.value.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  if (!formData.value.agreement) {
    uni.showToast({ title: '请同意用户协议', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const response = await uni.request({
      url: 'http://localhost:8080/api/user/login',
      method: 'POST',
      data: formData.value
    })

    if (response.data.code === 200) {
      const { token, user } = response.data.data
      userStore.setUser(user)
      userStore.setToken(token)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        router.push('/pages/index/index')
      }, 1000)
    } else {
      uni.showToast({ title: response.data.message || '登录失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '网络异常，请重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 去注册
const goToRegister = () => {
  router.push('/pages/register/register')
}
</script>

<style scoped>
/* 整体页面 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #F8F6F2;
  padding: 20px;
}

/* 登录卡片 */
.login-card {
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 40px 30px;
  width: 100%;
  max-width: 400px;
}

/* LOGO 区域 */
.logo-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40px;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.title-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.app-name {
  font-size: 28px;
  font-weight: 600;
  color: #C2977F;
  margin-bottom: 6px;
}

.app-slogan {
  font-size: 14px;
  color: #999;
}

/* 表单项 */
.form-item {
  margin-bottom: 24px;
}

.label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.input-box {
  width: 100%;
  height: 50px;
  border: 2px solid #E8E1D6;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 16px;
  box-sizing: border-box;
}

.input-box:focus {
  border-color: #C2977F;
}

/* 复选框 */
.checkbox-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.checkbox-text {
  font-size: 14px;
  color: #666;
  margin-left: 6px;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 50px;
  background-color: #C2977F;
  color: #fff;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
}

.login-btn:disabled {
  background-color: #D8C8BE;
}

/* 注册行 */
.register-row {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.link {
  color: #C2977F;
  margin-left: 6px;
}
</style>