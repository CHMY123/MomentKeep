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
          type="text"
          password
          placeholder="请输入密码"
          class="input-box"
        />
      </view>

      <!-- 记住我 -->
      <view class="checkbox-item">
        <checkbox-group @change="handleRememberChange">
          <checkbox :checked="formData.remember" value="remember" />
        </checkbox-group>
        <text class="checkbox-text">记住我</text>
      </view>

      <!-- 协议 -->
      <view class="checkbox-item">
        <checkbox-group @change="handleAgreementChange">
          <checkbox :checked="formData.agreement" value="agreement" />
        </checkbox-group>
        <text class="checkbox-text">我已阅读并同意</text>
        <text class="link" @click="showUserAgreement">用户协议</text>
        <text class="checkbox-text">和</text>
        <text class="link" @click="showPrivacyPolicy">隐私政策</text>
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
/**
 * MomentKeep 朝暮记 - 登录页面
 * @description 处理用户登录认证，包括表单验证、API调用和错误处理
 * @author MomentKeep Team
 * @since 2026-04-18
 */
import { ref, reactive } from 'vue'
import { useUserStore } from '../../store/user'
import { post } from '../../utils/request'

const userStore = useUserStore()

// 表单数据模型
const formData = ref({
  username: '',
  password: '',
  remember: false,
  agreement: false
})

// 加载状态
const loading = ref(false)

/**
 * 处理用户登录
 * @description 验证表单数据，调用登录API，成功后存储用户信息和Token
 */
const handleLogin = async () => {
  // 表单验证
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
    const res = await post('/user/login', formData.value)

    if (res.code === 200) {
      const { token, user } = res.data
      userStore.setUser(user)
      userStore.setToken(token)
      uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/index/index' })
        }, 1000)
    } else {
      uni.showToast({ title: res.message || '登录失败', icon: 'none' })
    }
  } catch (error) {
    console.error('登录失败:', error)
    uni.showToast({ title: '网络错误，请稍后重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到注册页面
 */
const goToRegister = () => {
  uni.navigateTo({ url: '/pages/register/register' })
}

/**
 * 显示用户协议弹窗
 */
const showUserAgreement = () => {
  uni.showModal({
    title: '用户协议',
    content: '用户协议内容...',
    showCancel: false,
    confirmText: '我知道了'
  })
}

/**
 * 显示隐私政策弹窗
 */
const showPrivacyPolicy = () => {
  uni.showModal({
    title: '隐私政策',
    content: '隐私政策内容...',
    showCancel: false,
    confirmText: '我知道了'
  })
}

/**
 * 处理"记住我"复选框变化
 */
const handleRememberChange = (e) => {
  formData.value.remember = e.detail.value.includes('remember')
}

/**
 * 处理用户协议复选框变化
 */
const handleAgreementChange = (e) => {
  formData.value.agreement = e.detail.value.includes('agreement')
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
  font-size: 12px;
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
  margin-top: 24px;
  display: block;
  margin-left: auto;
  margin-right: auto;
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
  font-size: 12px;
}
</style>